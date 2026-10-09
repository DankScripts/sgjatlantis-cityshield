# Locked 1.0.1-beta.2 baseline

This source tree corresponds to the user-tested and accepted Forge 1.20.1
`1.0.1-beta.2` release baseline locked on 2026-09-03. The exact accepted JAR,
checksum, and preservation contract are stored under
`baseline/accepted-balanced-bars/`.

The baseline includes full client lighting on only the four powered rear
lightbar faces:

- block light `15`;
- sky light `15`;
- ambient occlusion disabled.

The standby model still selects `atlantis_prop_lightbar_off`. Shield mechanics,
power use, controller behavior, and all other resources remain unchanged.

Locked release JAR SHA-256:

`87F204CC9994855546CD2ADA6B986135781508BD908CD025AC2696742FE5C1DD`

Future releases must use the accepted balanced-bars JAR as their direct binary
foundation. A successful build is not a new accepted baseline until it passes
in-game testing and receives explicit user acceptance.