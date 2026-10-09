package com.dankscripts.sgjatlantis_cityshield.shield;

import com.dankscripts.sgjatlantis_cityshield.AtlantisCityShield;
import com.dankscripts.sgjatlantis_cityshield.config.ShieldConfig;
import com.dankscripts.sgjatlantis_cityshield.network.ModNetwork;
import com.dankscripts.sgjatlantis_cityshield.network.ShieldImpactPacket;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.block.piston.PistonStructureResolver;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.ProjectileImpactEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.level.ExplosionEvent;
import net.minecraftforge.event.level.PistonEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;

@Mod.EventBusSubscriber(modid = AtlantisCityShield.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class ShieldProtectionEvents {
    private ShieldProtectionEvents() {}

    @SubscribeEvent
    public static void onEntityJoin(EntityJoinLevelEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level)
                || !(event.getEntity() instanceof LightningBolt lightning)) return;

        Vec3 strike = lightning.position();
        long gameTime = level.getGameTime();
        for (ShieldDefinition definition : activeShields(level)) {
            Vec3 impact = ShieldMath.upperSurfaceAt(definition, strike.x, strike.z);
            if (impact == null || !ShieldMath.isSurfaceFormed(definition, gameTime, impact)) continue;

            event.setCanceled(true);
            sendImpact(level, definition, impact, 1.55F);
            return;
        }
    }

    @SubscribeEvent
    public static void onLevelTick(TickEvent.LevelTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.level instanceof ServerLevel level)) return;

        long gameTime = level.getGameTime();
        List<ShieldDefinition> shields = activeShields(level);
        if (shields.isEmpty()) return;

        List<Projectile> absorbedProjectiles = new ArrayList<>();
        for (Entity entity : level.getAllEntities()) {
            if (!entity.isAlive() || entity.isRemoved() || entity.isPassenger()
                    || entity instanceof LightningBolt) continue;

            Vec3 previous = new Vec3(entity.xo, entity.yo, entity.zo);
            Vec3 current = entity.position();
            if (previous.distanceToSqr(current) < 1.0E-7) continue;

            for (ShieldDefinition definition : shields) {
                if (ShieldAccess.canCross(entity, definition)) continue;
                Vec3 impact = ShieldMath.formedBoundaryIntersection(definition, previous, current, gameTime);
                if (impact == null) continue;

                if (entity instanceof Projectile projectile) {
                    absorbedProjectiles.add(projectile);
                    float strength = (float) Math.min(1.8,
                            0.65 + projectile.getDeltaMovement().length() * 0.22);
                    sendImpact(level, definition, impact, strength);
                } else {
                    boolean survived = applyContactConsequence(
                            level, entity, definition, previous, impact);
                    if (!survived || entity.tickCount % 5 == 0) {
                        float strength = (float) Math.min(1.35,
                                0.45 + previous.distanceTo(current) * 0.18);
                        sendImpact(level, definition, impact, strength);
                    }
                }
                break;
            }
        }
        absorbedProjectiles.forEach(Entity::discard);
    }

    @SubscribeEvent
    public static void onExplosion(ExplosionEvent.Detonate event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        Vec3 origin = event.getExplosion().getPosition();

        long gameTime = level.getGameTime();
        for (ShieldDefinition definition : activeShields(level)) {
            Vec3 blockedTarget = event.getAffectedBlocks().stream()
                    .map(Vec3::atCenterOf)
                    .filter(position -> ShieldMath.formedBoundaryIntersection(
                            definition, origin, position, gameTime) != null)
                    .findFirst()
                    .orElseGet(() -> event.getAffectedEntities().stream()
                            .map(entity -> entity.getBoundingBox().getCenter())
                            .filter(position -> ShieldMath.formedBoundaryIntersection(
                                    definition, origin, position, gameTime) != null)
                            .findFirst()
                            .orElse(null));

            int blockCount = event.getAffectedBlocks().size();
            int entityCount = event.getAffectedEntities().size();
            event.getAffectedBlocks().removeIf(position -> ShieldMath.formedBoundaryIntersection(
                    definition, origin, Vec3.atCenterOf(position), gameTime) != null);
            event.getAffectedEntities().removeIf(entity -> ShieldMath.formedBoundaryIntersection(
                    definition, origin, entity.getBoundingBox().getCenter(), gameTime) != null);
            int blocked = blockCount - event.getAffectedBlocks().size()
                    + entityCount - event.getAffectedEntities().size();
            if (blocked <= 0 || blockedTarget == null) continue;

            Vec3 impact = ShieldMath.formedBoundaryIntersection(definition, origin, blockedTarget, gameTime);
            if (impact != null) sendImpact(level, definition, impact,
                    (float) Math.min(2.25, 0.8 + blocked * 0.035));
        }
    }

    @SubscribeEvent
    public static void onProjectileImpact(ProjectileImpactEvent event) {
        Projectile projectile = event.getProjectile();
        if (!(projectile.level() instanceof ServerLevel level) || projectile.isRemoved()) return;
        Vec3 previous = new Vec3(projectile.xo, projectile.yo, projectile.zo);
        Vec3 destination = event.getRayTraceResult().getLocation();

        long gameTime = level.getGameTime();
        for (ShieldDefinition definition : activeShields(level)) {
            if (ShieldAccess.canCross(projectile, definition)) continue;
            Vec3 impact = ShieldMath.formedBoundaryIntersection(
                    definition, previous, destination, gameTime);
            if (impact == null) continue;

            event.setImpactResult(ProjectileImpactEvent.ImpactResult.STOP_AT_CURRENT_NO_DAMAGE);
            projectile.discard();
            float strength = (float) Math.min(1.8,
                    0.65 + projectile.getDeltaMovement().length() * 0.22);
            sendImpact(level, definition, impact, strength);
            return;
        }
    }

    @SubscribeEvent
    public static void onLivingAttack(LivingAttackEvent event) {
        if (!(event.getEntity().level() instanceof ServerLevel level)) return;
        Entity direct = event.getSource().getDirectEntity();
        Entity attacker = event.getSource().getEntity();
        Vec3 origin = direct != null ? direct.getBoundingBox().getCenter()
                : attacker != null ? attacker.getBoundingBox().getCenter()
                : event.getSource().getSourcePosition();
        if (origin == null) return;

        Vec3 target = event.getEntity().getBoundingBox().getCenter();
        long gameTime = level.getGameTime();
        for (ShieldDefinition definition : activeShields(level)) {
            Vec3 impact = ShieldMath.formedBoundaryIntersection(definition, origin, target, gameTime);
            if (impact == null) continue;

            event.setCanceled(true);
            sendImpact(level, definition, impact,
                    (float) Math.min(1.8, 0.5 + event.getAmount() * 0.08));
            return;
        }
    }

    @SubscribeEvent
    public static void onPistonMove(PistonEvent.Pre event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        PistonStructureResolver resolver = event.getStructureHelper();
        if (resolver == null || !resolver.resolve()) return;

        Direction movement = event.getPistonMoveType().isExtend
                ? event.getDirection()
                : event.getDirection().getOpposite();
        List<BlockPos> movingBlocks = new ArrayList<>(resolver.getToPush());
        if (event.getPistonMoveType().isExtend) movingBlocks.add(event.getPos());

        long gameTime = level.getGameTime();
        for (ShieldDefinition definition : activeShields(level)) {
            for (BlockPos source : movingBlocks) {
                BlockPos destination = source.relative(movement);
                Vec3 impact = ShieldMath.formedBoundaryIntersection(definition,
                        Vec3.atCenterOf(source), Vec3.atCenterOf(destination), gameTime);
                if (impact == null) continue;

                event.setCanceled(true);
                sendImpact(level, definition, impact, 0.75F);
                return;
            }
        }
    }

    private static List<ShieldDefinition> activeShields(ServerLevel level) {
        return ShieldSavedData.get(level.getServer()).snapshot().stream()
                .filter(definition -> ShieldMath.isInDimension(definition, level.dimension().location()))
                .filter(ShieldDefinition::active)
                .toList();
    }

    private static boolean applyContactConsequence(ServerLevel level, Entity entity,
                                                   ShieldDefinition definition,
                                                   Vec3 previous, Vec3 impact) {
        if (ShieldConfig.ENTITY_CONTACT_MODE.get()
                == ShieldConfig.EntityContactMode.INSTANT_DEATH) {
            entity.kill();
            return false;
        }

        float damage = ShieldConfig.MAJOR_CONTACT_DAMAGE.get().floatValue();
        entity.hurt(level.damageSources().genericKill(), damage);
        if (!entity.isAlive() || entity.isRemoved()) return false;

        repelToPreviousSide(entity, definition, previous, impact);
        return true;
    }

    private static void repelToPreviousSide(Entity entity, ShieldDefinition definition,
                                            Vec3 previous, Vec3 impact) {
        Vec3 towardPrevious = previous.subtract(impact);
        if (towardPrevious.lengthSqr() < 1.0E-6) {
            towardPrevious = ShieldMath.contains(definition, previous)
                    ? ShieldMath.center(definition).subtract(impact)
                    : impact.subtract(ShieldMath.center(definition));
        }
        if (towardPrevious.lengthSqr() < 1.0E-6) towardPrevious = new Vec3(0.0, 1.0, 0.0);

        double clearance = Math.max(0.65, entity.getBbWidth() * 0.75);
        Vec3 safePosition = impact.add(towardPrevious.normalize().scale(clearance));
        entity.teleportTo(safePosition.x, safePosition.y, safePosition.z);
        entity.setDeltaMovement(Vec3.ZERO);
        entity.fallDistance = 0.0F;
        if (entity instanceof Mob mob) mob.getNavigation().stop();
    }

    private static void sendImpact(ServerLevel level, ShieldDefinition definition, Vec3 impact, float strength) {
        ShieldImpactPacket packet = new ShieldImpactPacket(definition.dimension(), definition.controllerPos().asLong(),
                impact.x, impact.y, impact.z, strength, level.getGameTime());
        ModNetwork.CHANNEL.send(PacketDistributor.DIMENSION.with(level::dimension), packet);
    }
}
