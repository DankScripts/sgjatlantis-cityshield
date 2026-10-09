#!/usr/bin/env python3
"""Build a prop-faithful Atlantis shield console as an editable bbmodel."""

from __future__ import annotations

import base64
import io
import json
import math
import uuid
from pathlib import Path

from PIL import Image, ImageDraw


ROOT = Path(__file__).resolve().parents[1]
SOURCE = ROOT / "blockbench" / "atlantis_dhd_table_source.bbmodel"
OUTPUT = ROOT / "blockbench" / "atlantis_city_shield_control_table_v3_pedestal.bbmodel"
NAMESPACE = uuid.UUID("a98a880d-a98d-4f69-8bc4-0fb61603efc6")


def uid(kind: str, name: str) -> str:
    return str(uuid.uuid5(NAMESPACE, f"{kind}:{name}"))


def data_url(image: Image.Image) -> str:
    stream = io.BytesIO()
    image.save(stream, "PNG", optimize=True)
    return "data:image/png;base64," + base64.b64encode(stream.getvalue()).decode("ascii")


def texture_image(kind: str) -> Image.Image:
    im = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    d = ImageDraw.Draw(im)

    if kind == "shell":
        d.rectangle((0, 0, 15, 15), fill=(67, 36, 33, 255))
        d.rectangle((0, 0, 15, 15), outline=(127, 67, 50, 255), width=1)
        d.line((1, 12, 5, 8, 10, 8, 14, 4), fill=(101, 51, 43, 255), width=1)
        d.line((2, 3, 6, 3, 9, 1), fill=(94, 49, 41, 255), width=1)
    elif kind == "fascia":
        d.rectangle((0, 0, 15, 15), fill=(73, 39, 35, 255))
        d.polygon(((1, 4), (6, 3), (8, 8), (5, 13), (1, 11)), fill=(132, 68, 50, 255))
        d.polygon(((9, 2), (14, 3), (14, 11), (10, 13), (8, 8)), fill=(112, 57, 47, 255))
        d.line((0, 1, 15, 1), fill=(167, 86, 59, 255), width=1)
        d.line((0, 14, 15, 14), fill=(38, 25, 29, 255), width=1)
    elif kind == "deck":
        d.rectangle((0, 0, 15, 15), fill=(43, 29, 31, 255))
        d.rectangle((0, 0, 15, 15), outline=(96, 52, 44, 255), width=1)
        d.line((1, 12, 4, 9, 9, 9, 14, 4), fill=(103, 56, 45, 255), width=1)
        d.line((2, 5, 7, 5, 10, 2), fill=(82, 43, 39, 255), width=1)
    elif kind == "inlay":
        d.rectangle((0, 0, 15, 15), fill=(25, 21, 25, 255))
        d.rectangle((1, 1, 14, 14), outline=(97, 54, 44, 255), width=1)
        d.line((2, 12, 6, 8, 10, 8, 13, 5), fill=(71, 42, 39, 255), width=1)
    elif kind == "light":
        for y in range(16):
            for x in range(16):
                dist = abs(y - 7.5)
                glow = max(0, 1.0 - dist / 8.0)
                im.putpixel((x, y), (int(196 + 59 * glow), int(225 + 30 * glow), 255, 255))
        d.rectangle((0, 0, 15, 15), outline=(113, 180, 204, 255), width=1)
    elif kind == "crystal":
        d.polygon(((2, 1), (13, 1), (15, 5), (12, 14), (4, 15), (0, 10), (1, 4)), fill=(211, 234, 230, 205))
        d.line((2, 2, 12, 2, 14, 5), fill=(255, 255, 249, 245), width=1)
        d.line((3, 13, 11, 12, 14, 6), fill=(130, 189, 191, 225), width=1)
        d.line((3, 4, 8, 7, 12, 4), fill=(255, 250, 222, 175), width=1)
    elif kind == "lit_crystal":
        d.rectangle((0, 0, 15, 15), fill=(211, 241, 238, 255))
        d.rectangle((0, 0, 15, 15), outline=(105, 190, 198, 255), width=1)
        d.polygon(((2, 2), (12, 2), (14, 5), (11, 13), (4, 14), (1, 10)),
                  fill=(224, 248, 244, 255))
        d.line((2, 3, 11, 3, 13, 5), fill=(255, 255, 250, 255), width=1)
        d.line((4, 12, 10, 11, 13, 6), fill=(133, 205, 208, 255), width=1)
    elif kind == "shield":
        for y in range(16):
            for x in range(16):
                wave = ((x * 7 + y * 5) % 13) < 4
                im.putpixel((x, y), (35, 201 + (34 if wave else 0), 243, 185))
        d.rectangle((0, 0, 15, 15), outline=(189, 252, 255, 235), width=1)
        d.polygon(((8, 1), (14, 5), (13, 12), (8, 15), (2, 11), (1, 5)), outline=(185, 252, 255, 210))
    elif kind == "amber":
        d.rectangle((0, 0, 15, 15), fill=(106, 48, 30, 255))
        d.rectangle((2, 2, 13, 13), fill=(219, 113, 52, 255))
        d.line((3, 11, 7, 7, 12, 7), fill=(255, 220, 154, 255), width=1)
    else:
        raise ValueError(kind)
    return im


def texture(name: str, kind: str, index: int) -> dict:
    return {
        "name": name,
        "path": "",
        "folder": "",
        "namespace": "",
        "id": str(index),
        "group": "",
        "scope": 0,
        "width": 16,
        "height": 16,
        "uv_width": 16,
        "uv_height": 16,
        "particle": False,
        "use_as_default": index == 0,
        "layers_enabled": False,
        "sync_to_project": "",
        "file_format": "png",
        "render_mode": "default",
        "render_sides": "auto",
        "wrap_mode": "limited",
        "pbr_channel": "color",
        "fps": 7,
        "frame_time": 1,
        "frame_order_type": "loop",
        "frame_order": "",
        "frame_interpolate": False,
        "visible": True,
        "internal": True,
        "saved": True,
        "uuid": uid("texture", name),
        "source": data_url(texture_image(kind)),
    }


def uv(a: list[float], b: list[float], direction: str) -> list[float]:
    dx = min(abs(b[0] - a[0]), 16)
    dy = min(abs(b[1] - a[1]), 16)
    dz = min(abs(b[2] - a[2]), 16)
    if direction in ("north", "south"):
        return [0, 0, dx, dy]
    if direction in ("east", "west"):
        return [0, 0, dz, dy]
    return [0, 0, dx, dz]


def cube(
    name: str,
    start: tuple[float, float, float],
    end: tuple[float, float, float],
    tex: int,
    *,
    color: int = 0,
    light: int = 0,
    rotation: float | None = None,
    axis: str = "y",
    origin: tuple[float, float, float] | None = None,
    shade: bool = True,
) -> dict:
    a = [round(v, 5) for v in start]
    b = [round(v, 5) for v in end]
    center = origin or tuple((a[i] + b[i]) / 2 for i in range(3))
    faces = {direction: {"uv": uv(a, b, direction), "texture": tex} for direction in ("north", "east", "south", "west", "up", "down")}
    result = {
        "name": name,
        "box_uv": False,
        "render_order": "default",
        "rescale": False,
        "locked": False,
        "shade": shade,
        "light_emission": light,
        "export": True,
        "scope": 0,
        "allow_mirror_modeling": True,
        "from": a,
        "to": b,
        "autouv": 0,
        "color": color,
        "origin": [round(v, 5) for v in center],
        "faces": faces,
        "type": "cube",
        "uuid": uid("cube", name),
    }
    if rotation is not None:
        result["rotation"] = rotation
        result["axis"] = axis
    return result


def group(name: str, origin: tuple[float, float, float], color: int = 0) -> dict:
    return {
        "name": name,
        "uuid": uid("group", name),
        "export": True,
        "locked": False,
        "scope": 0,
        "selected": False,
        "_static": {"properties": {}, "temp_data": {}},
        "origin": list(origin),
        "rotation": [0, 0, 0],
        "color": color,
        "children": [],
        "reset": False,
        "shade": True,
        "mirror_uv": False,
        "visibility": True,
        "autouv": 0,
        "isOpen": True,
        "primary_selected": False,
    }


def tree_group(name: str, children: list, opened: bool = True) -> dict:
    return {"uuid": uid("group", name), "isOpen": opened, "children": children}


def add_crystal_module(items: list[dict], name: str, x: float, z: float, width: float = 1.75, depth: float = 1.45) -> None:
    origin = (16.0, 15.75, 8.0)
    items.extend(
        [
            cube(f"{name}_socket", (x - width / 2 - 0.16, 16.85, z - depth / 2 - 0.14), (x + width / 2 + 0.16, 17.02, z + depth / 2 + 0.14), 3, color=3, rotation=-8, axis="x", origin=origin),
            cube(f"{name}_crystal", (x - width / 2, 17.01, z - depth / 2), (x + width / 2, 17.38, z + depth / 2), 8, color=5, light=9, rotation=-8, axis="x", origin=origin, shade=False),
            cube(f"{name}_ridge", (x - width * 0.22, 17.36, z + depth * 0.05), (x + width * 0.22, 17.76, z + depth * 0.42), 8, color=5, light=10, rotation=-8, axis="x", origin=origin, shade=False),
        ]
    )


def build() -> None:
    model = json.loads(SOURCE.read_text(encoding="utf-8"))
    model["name"] = "atlantis_city_shield_control_table_v3_pedestal"
    model["elements"] = []
    model["groups"] = []
    model["outliner"] = []
    model["textures"] = [
        texture("atlantis_prop_shell.png", "shell", 0),
        texture("atlantis_prop_fascia.png", "fascia", 1),
        texture("atlantis_prop_deck.png", "deck", 2),
        texture("atlantis_prop_inlay.png", "inlay", 3),
        texture("atlantis_prop_lightbar.png", "light", 4),
        texture("atlantis_prop_crystal.png", "crystal", 5),
        texture("atlantis_shield_projector.png", "shield", 6),
        texture("atlantis_prop_amber_glyph.png", "amber", 7),
        texture("atlantis_prop_lit_crystal.png", "lit_crystal", 8),
    ]

    sections: dict[str, list[dict]] = {
        "sled_supports": [],
        "console_shell": [],
        "sloped_control_deck": [],
        "rear_lightbar": [],
        "lantean_crystal_controls": [],
        "shield_projector": [],
        "primary_controls": [],
        "ancient_detailing": [],
    }

    # Two straight T-pedestals support the prop. The horizontal lower crossbeam
    # connects the legs when viewed from the front; neither side profile is Z-shaped.
    for side, x0, x1 in (("left", 4.3, 6.5), ("right", 25.5, 27.7)):
        sections["sled_supports"].extend(
            [
                cube(f"{side}_sled_foot", (x0, 0.15, 2.6), (x1, 1.35, 16.2), 1, color=1),
                cube(f"{side}_pedestal_upright", (x0 + 0.15, 1.1, 7.0), (x1 - 0.15, 14.4, 9.0), 0, color=0),
                cube(f"{side}_upper_mount", (x0 - 0.1, 12.55, 3.0), (x1 + 0.1, 14.25, 12.6), 1, color=1),
                cube(f"{side}_foot_inlay", (x0 + 0.35, 1.36, 4.0), (x1 - 0.35, 1.54, 14.8), 7, color=7, light=2),
            ]
        )
    sections["sled_supports"].extend(
        [
            cube("lower_crossbeam", (6.4, 6.5, 7.15), (25.6, 8.1, 9.15), 3, color=3),
            cube("underside_power_spine", (8.2, 11.9, 5.1), (23.8, 13.25, 11.7), 0, color=0),
        ]
    )

    # Thick, dark-copper wedge with stepped rear shoulders and a narrow front.
    sections["console_shell"].extend(
        [
            cube("console_wedge_core", (4.1, 14.85, 2.25), (27.9, 16.45, 13.55), 0, color=0, rotation=-8, axis="x", origin=(16.0, 15.75, 8.0)),
            cube("front_fascia_center", (5.7, 13.95, 1.35), (26.3, 16.0, 3.2), 1, color=1),
            cube("front_fascia_left_chamfer", (3.4, 13.95, 1.75), (7.2, 16.0, 3.15), 1, color=1, rotation=-22.5, axis="y", origin=(5.3, 14.98, 2.45)),
            cube("front_fascia_right_chamfer", (24.8, 13.95, 1.75), (28.6, 16.0, 3.15), 1, color=1, rotation=22.5, axis="y", origin=(26.7, 14.98, 2.45)),
            cube("front_fascia_lower_shadow", (5.8, 13.58, 1.62), (26.2, 14.2, 2.82), 3, color=3),
            cube("left_side_fascia_front", (3.4, 14.0, 2.7), (5.2, 16.2, 7.0), 1, color=1),
            cube("left_side_fascia_mid", (2.8, 14.35, 6.6), (5.0, 16.75, 10.4), 1, color=1),
            cube("left_side_fascia_rear", (2.2, 14.75, 10.0), (5.0, 17.55, 14.6), 1, color=1),
            cube("right_side_fascia_front", (26.8, 14.0, 2.7), (28.6, 16.2, 7.0), 1, color=1),
            cube("right_side_fascia_mid", (27.0, 14.35, 6.6), (29.2, 16.75, 10.4), 1, color=1),
            cube("right_side_fascia_rear", (27.0, 14.75, 10.0), (29.8, 17.55, 14.6), 1, color=1),
            cube("rear_housing", (2.1, 16.15, 12.9), (29.9, 18.7, 15.45), 0, color=0),
            cube("left_rear_endcap", (1.65, 15.7, 13.25), (3.25, 18.85, 15.8), 1, color=1),
            cube("right_rear_endcap", (28.75, 15.7, 13.25), (30.35, 18.85, 15.8), 1, color=1),
        ]
    )

    # A thin sloped top plane. Rear shoulders widen just like the show prop.
    deck_origin = (16.0, 15.75, 8.0)
    sections["sloped_control_deck"].extend(
        [
            cube("deck_center", (4.15, 16.42, 2.25), (27.85, 16.72, 13.45), 2, color=2, rotation=-8, axis="x", origin=deck_origin),
            cube("deck_left_rear_shoulder", (2.55, 16.42, 6.2), (4.5, 16.72, 13.45), 2, color=2, rotation=-8, axis="x", origin=deck_origin),
            cube("deck_right_rear_shoulder", (27.5, 16.42, 6.2), (29.45, 16.72, 13.45), 2, color=2, rotation=-8, axis="x", origin=deck_origin),
            cube("deck_front_inlay", (7.2, 16.72, 2.8), (24.8, 16.88, 5.4), 3, color=3, rotation=-8, axis="x", origin=deck_origin),
            cube("deck_left_bank_inlay", (5.2, 16.72, 6.0), (13.7, 16.88, 12.65), 3, color=3, rotation=-8, axis="x", origin=deck_origin),
            cube("deck_right_bank_inlay", (18.3, 16.72, 6.0), (26.8, 16.88, 12.65), 3, color=3, rotation=-8, axis="x", origin=deck_origin),
        ]
    )

    sections["rear_lightbar"].extend(
        [
            cube("rear_lightbar_face", (3.65, 18.05, 12.55), (28.35, 19.15, 13.28), 4, color=4, light=15, shade=False),
            cube("rear_lightbar_cap", (3.0, 19.14, 12.85), (29.0, 19.62, 14.35), 0, color=0),
            cube("rear_lightbar_left_cap", (2.75, 17.75, 12.55), (4.05, 19.35, 14.15), 1, color=1),
            cube("rear_lightbar_right_cap", (27.95, 17.75, 12.55), (29.25, 19.35, 14.15), 1, color=1),
        ]
    )

    # Staggered translucent paddles based on the physical Atlantis console props.
    for i, x in enumerate((6.7, 9.25, 11.8, 20.2, 22.75, 25.3), start=1):
        add_crystal_module(sections["lantean_crystal_controls"], f"rear_crystal_{i:02d}", x, 10.85)
    for i, x in enumerate((7.35, 10.0, 12.65, 19.35, 22.0, 24.65), start=1):
        add_crystal_module(sections["lantean_crystal_controls"], f"middle_crystal_{i:02d}", x, 8.15, width=1.9, depth=1.55)

    # Recessed octagonal shield projector. The future renderer will spawn the
    # smooth active hologram above this anchor; no permanent cube-dome is used.
    sections["shield_projector"].extend(
        [
            cube("projector_front", (14.1, 17.35, 5.1), (17.9, 17.57, 5.5), 0, color=0, rotation=-8, axis="x", origin=deck_origin),
            cube("projector_rear", (14.1, 17.35, 7.75), (17.9, 17.57, 8.15), 0, color=0, rotation=-8, axis="x", origin=deck_origin),
            cube("projector_left", (13.15, 17.35, 5.9), (13.55, 17.57, 7.35), 0, color=0, rotation=-8, axis="x", origin=deck_origin),
            cube("projector_right", (18.45, 17.35, 5.9), (18.85, 17.57, 7.35), 0, color=0, rotation=-8, axis="x", origin=deck_origin),
            cube("projector_inner_left", (13.55, 17.37, 5.45), (16.0, 17.53, 7.8), 6, color=6, light=8, rotation=-8, axis="x", origin=deck_origin, shade=False),
            cube("projector_inner_right", (16.0, 17.37, 5.45), (18.45, 17.53, 7.8), 6, color=6, light=8, rotation=-8, axis="x", origin=deck_origin, shade=False),
            cube("shield_hologram_anchor", (15.75, 17.53, 6.35), (16.25, 17.78, 6.9), 6, color=6, light=11, rotation=-8, axis="x", origin=deck_origin, shade=False),
        ]
    )

    # Two larger prop-style crystal paddles map to Shield Control and Settings.
    add_crystal_module(sections["primary_controls"], "shield_control", 10.15, 4.35, width=2.65, depth=1.55)
    add_crystal_module(sections["primary_controls"], "settings_control", 21.85, 4.35, width=2.65, depth=1.55)
    sections["primary_controls"].extend(
        [
            cube("shield_control_underlight", (8.65, 16.7, 3.45), (11.65, 16.82, 3.75), 6, color=6, light=8, rotation=-8, axis="x", origin=deck_origin, shade=False),
            cube("settings_control_underlight", (20.35, 16.7, 3.45), (23.35, 16.82, 3.75), 7, color=7, light=5, rotation=-8, axis="x", origin=deck_origin, shade=False),
        ]
    )

    # Small Ancient glyph cluster and fascia appliques echo the production prop.
    for row in range(3):
        for col in range(4):
            x = 5.2 + col * 0.72
            z = 3.55 + row * 0.58
            sections["ancient_detailing"].append(
                cube(f"ancient_key_{row + 1}_{col + 1}", (x, 16.78, z), (x + 0.38, 16.96, z + 0.32), 7, color=7, light=2, rotation=-8, axis="x", origin=deck_origin)
            )
    for i, x in enumerate((6.0, 10.0, 14.0, 18.0, 22.0), start=1):
        sections["ancient_detailing"].append(cube(f"front_fascia_panel_{i}", (x, 14.45, 1.22), (x + 2.7, 15.35, 1.42), 1, color=1))

    origins = {
        "sled_supports": (16.0, 7.0, 8.0),
        "console_shell": (16.0, 15.5, 8.0),
        "sloped_control_deck": (16.0, 16.0, 8.0),
        "rear_lightbar": (16.0, 18.5, 13.5),
        "lantean_crystal_controls": (16.0, 17.0, 9.5),
        "shield_projector": (16.0, 17.0, 6.65),
        "primary_controls": (16.0, 17.0, 4.35),
        "ancient_detailing": (16.0, 15.5, 4.0),
    }
    colors = {name: index % 8 for index, name in enumerate(sections, start=1)}

    root_children = []
    for name, items in sections.items():
        model["elements"].extend(items)
        model["groups"].append(group(name, origins[name], colors[name]))
        root_children.append(tree_group(name, [item["uuid"] for item in items], opened=name in {"console_shell", "shield_projector", "primary_controls"}))

    root = "atlantis_city_shield_control_table_v3"
    model["groups"].append(group(root, (16.0, 10.0, 8.0), 0))
    model["outliner"] = [tree_group(root, root_children)]

    # Structural validation for Blockbench's editable project format.
    element_ids = [item["uuid"] for item in model["elements"]]
    if len(element_ids) != len(set(element_ids)):
        raise RuntimeError("duplicate element UUID")
    if any(item["to"][i] <= item["from"][i] for item in model["elements"] for i in range(3)):
        raise RuntimeError("invalid cube bounds")
    if any(face["texture"] >= len(model["textures"]) for item in model["elements"] for face in item["faces"].values()):
        raise RuntimeError("invalid texture reference")

    referenced_elements: list[str] = []
    referenced_groups: list[str] = []

    def walk(node) -> None:
        if isinstance(node, str):
            referenced_elements.append(node)
        else:
            referenced_groups.append(node["uuid"])
            for child in node.get("children", []):
                walk(child)

    for node in model["outliner"]:
        walk(node)
    if set(element_ids) != set(referenced_elements):
        raise RuntimeError("outliner does not reference every element exactly")
    if {item["uuid"] for item in model["groups"]} != set(referenced_groups):
        raise RuntimeError("group metadata does not match outliner")
    if any("city_button" in item["name"] for item in model["elements"]):
        raise RuntimeError("DHD geometry leaked into v2")

    OUTPUT.write_text(json.dumps(model, indent=2) + "\n", encoding="utf-8")
    print(f"Wrote {OUTPUT}")
    print(f"Elements: {len(model['elements'])}")
    print(f"Groups: {len(model['groups'])}")
    print(f"Embedded textures: {len(model['textures'])}")


if __name__ == "__main__":
    build()
