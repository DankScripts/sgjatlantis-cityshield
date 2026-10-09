#!/usr/bin/env python3
"""Compose and export the approved Blockbench shield console."""

from __future__ import annotations

import base64
import io
import json
from pathlib import Path

from PIL import Image


ROOT = Path(__file__).resolve().parents[1]
SOURCE = ROOT / "blockbench" / "atlantis_city_shield_control_table_v2_prop_style.bbmodel"
CRYSTAL_REFERENCE = ROOT / "blockbench" / "atlantis_city_shield_control_table.bbmodel"
DHD_CRYSTAL_TEXTURE = ROOT / "blockbench" / "dhd_crystal_pattern.png"
COMPOSED_SOURCE = ROOT / "blockbench" / "atlantis_city_shield_control_table_final.bbmodel"
MODEL_DIR = ROOT / "src/main/resources/assets/sgjatlantis_cityshield/models/block"
TEXTURE_DIR = ROOT / "src/main/resources/assets/sgjatlantis_cityshield/textures/block"

TEXTURE_KEYS = {
    0: "shell",
    1: "fascia",
    2: "deck",
    3: "inlay",
    4: "lightbar",
    5: "crystal",
    6: "projector",
    7: "amber",
    8: "lit_crystal",
}

LIT_CRYSTAL_ELEMENTS = {
    *(f"rear_crystal_{index:02d}_crystal" for index in range(1, 7)),
    *(f"middle_crystal_{index:02d}_crystal" for index in range(1, 7)),
}

# Match the completed Atlantis DHD table's two-block prop envelope.  The
# original shield console was slightly narrower, shorter, and shallower even
# though the two tables are intended to sit together in the same control room.
TARGET_MODEL_MIN = (0.0, 0.0, 0.0)
TARGET_MODEL_MAX = (32.0, 20.0, 16.25)

ITEM_DISPLAY = {
    "thirdperson_righthand": {
        "rotation": [68, 45, 0], "translation": [0, 2, 0], "scale": [0.28, 0.28, 0.28]
    },
    "thirdperson_lefthand": {
        "rotation": [68, 225, 0], "translation": [0, 2, 0], "scale": [0.28, 0.28, 0.28]
    },
    "firstperson_righthand": {
        "rotation": [12, 45, 0], "translation": [0, 1, 0], "scale": [0.25, 0.25, 0.25]
    },
    "firstperson_lefthand": {
        "rotation": [12, 225, 0], "translation": [0, 1, 0], "scale": [0.25, 0.25, 0.25]
    },
    "ground": {"translation": [0, 2, 0], "scale": [0.34, 0.34, 0.34]},
    "gui": {"rotation": [32, 225, 0], "translation": [0, -1, 0], "scale": [0.42, 0.42, 0.42]},
    "fixed": {"rotation": [0, 180, 0], "translation": [0, -1, 0], "scale": [0.45, 0.45, 0.45]},
}


def compact_number(value: float) -> int | float:
    rounded = round(value, 5)
    return int(rounded) if float(rounded).is_integer() else rounded


def coordinates(values: list[float], translate_x: bool = False) -> list[int | float]:
    result = list(values)
    if translate_x:
        # The Blockbench work area was 32 px wide. Center it over the placed
        # controller block so blockstate rotation pivots around the console.
        result[0] -= 8.0
    return [compact_number(value) for value in result]


def tinted(image: Image.Image, color: tuple[int, int, int], brightness: float) -> Image.Image:
    source = image.convert("RGBA")
    output = Image.new("RGBA", source.size)
    source_pixels = source.load()
    output_pixels = output.load()
    for y in range(source.height):
        for x in range(source.width):
            red, green, blue, alpha = source_pixels[x, y]
            intensity = max(red, green, blue) / 255.0
            output_pixels[x, y] = (
                int(color[0] * intensity * brightness),
                int(color[1] * intensity * brightness),
                int(color[2] * intensity * brightness),
                alpha,
            )
    return output


def export_textures(project: dict) -> dict[int, str]:
    TEXTURE_DIR.mkdir(parents=True, exist_ok=True)
    names: dict[int, str] = {}
    images: dict[int, Image.Image] = {}
    for index, texture in enumerate(project["textures"]):
        name = Path(texture["name"]).stem
        names[index] = name
        encoded = texture["source"].split(",", 1)[1]
        image = Image.open(io.BytesIO(base64.b64decode(encoded))).convert("RGBA")
        images[index] = image
        output_path = TEXTURE_DIR / f"{name}.png"
        if index == 8:
            output_path.write_bytes(DHD_CRYSTAL_TEXTURE.read_bytes())
        else:
            image.save(output_path)

    tinted(images[4], (74, 115, 128), 0.38).save(TEXTURE_DIR / "atlantis_prop_lightbar_off.png")
    tinted(images[6], (31, 92, 103), 0.34).save(TEXTURE_DIR / "atlantis_shield_projector_off.png")
    tinted(images[4], (255, 126, 38), 0.92).save(TEXTURE_DIR / "atlantis_prop_lightbar_warning.png")
    tinted(images[6], (255, 88, 24), 0.82).save(TEXTURE_DIR / "atlantis_shield_projector_warning.png")
    return names


def scale_to_dhd_table(project: dict) -> None:
    """Scale every model-space point into the DHD table's full envelope."""
    elements = [element for element in project["elements"] if element.get("export", True)]
    source_min = [min(element["from"][axis] for element in elements) for axis in range(3)]
    source_max = [max(element["to"][axis] for element in elements) for axis in range(3)]
    factors = [
        (TARGET_MODEL_MAX[axis] - TARGET_MODEL_MIN[axis])
        / (source_max[axis] - source_min[axis])
        for axis in range(3)
    ]

    def transform(point: list[float]) -> list[int | float]:
        return [
            compact_number(
                TARGET_MODEL_MIN[axis]
                + (point[axis] - source_min[axis]) * factors[axis]
            )
            for axis in range(3)
        ]

    for element in project["elements"]:
        element["from"] = transform(element["from"])
        element["to"] = transform(element["to"])
        if "origin" in element:
            element["origin"] = transform(element["origin"])


def export_element(element: dict) -> dict:
    result: dict = {
        "from": coordinates(element["from"], True),
        "to": coordinates(element["to"], True),
        "faces": {},
    }
    if element.get("shade") is False:
        result["shade"] = False
    if "rotation" in element:
        result["rotation"] = {
            "angle": compact_number(element["rotation"]),
            "axis": element.get("axis", "y"),
            "origin": coordinates(element["origin"], True),
            "rescale": bool(element.get("rescale", False)),
        }
    for direction, face in element["faces"].items():
        texture_index = int(face["texture"])
        exported_face = {
            "uv": coordinates(face["uv"]),
            "texture": f"#{TEXTURE_KEYS[texture_index]}",
        }
        if face.get("rotation"):
            exported_face["rotation"] = face["rotation"]
        result["faces"][direction] = exported_face
    return result


def write_json(path: Path, value: dict) -> None:
    path.write_text(json.dumps(value, indent=2) + "\n", encoding="utf-8")


def compose_project() -> dict:
    project = json.loads(SOURCE.read_text(encoding="utf-8"))
    reference = json.loads(CRYSTAL_REFERENCE.read_text(encoding="utf-8"))
    lit_texture = dict(next(
        texture for texture in reference["textures"]
        if texture["name"] == "atlantis_prop_lit_crystal.png"
    ))
    lit_texture["source"] = (
        "data:image/png;base64,"
        + base64.b64encode(DHD_CRYSTAL_TEXTURE.read_bytes()).decode("ascii")
    )
    with Image.open(DHD_CRYSTAL_TEXTURE) as crystal_pattern:
        lit_texture["width"] = crystal_pattern.width
        lit_texture["height"] = crystal_pattern.height
        lit_texture["uv_width"] = 16
        lit_texture["uv_height"] = 16
    project["textures"] = [
        texture for texture in project["textures"]
        if texture["name"] != "atlantis_prop_lit_crystal.png"
    ]
    project["textures"].append(lit_texture)
    lit_texture_index = len(project["textures"]) - 1
    for element in project["elements"]:
        if element["name"] not in LIT_CRYSTAL_ELEMENTS:
            continue
        for face in element["faces"].values():
            face["texture"] = lit_texture_index
        # The DHD crystal pattern is designed to fill an entire pad.  Map the
        # complete pattern across the visible top instead of sampling a tiny
        # corner according to the pad's physical dimensions.
        element["faces"]["up"]["uv"] = [0, 0, 16, 16]
        element["light_emission"] = 12
    scale_to_dhd_table(project)
    write_json(COMPOSED_SOURCE, project)
    return project


def main() -> None:
    project = compose_project()
    texture_names = export_textures(project)
    MODEL_DIR.mkdir(parents=True, exist_ok=True)

    textures = {
        "particle": "sgjatlantis_cityshield:block/atlantis_prop_shell",
        **{
            key: f"sgjatlantis_cityshield:block/{texture_names[index]}"
            for index, key in TEXTURE_KEYS.items()
        },
    }
    base = {
        "parent": "minecraft:block/block",
        "ambientocclusion": True,
        "textures": textures,
        "elements": [
            export_element(element)
            for element in project["elements"]
            if element.get("export", True)
        ],
        "display": ITEM_DISPLAY,
    }
    write_json(MODEL_DIR / "shield_controller_base.json", base)
    write_json(MODEL_DIR / "shield_controller_standby.json", {
        "parent": "sgjatlantis_cityshield:block/shield_controller_base",
        "textures": {
            "lightbar": "sgjatlantis_cityshield:block/atlantis_prop_lightbar_off",
            "projector": "sgjatlantis_cityshield:block/atlantis_shield_projector_off",
        },
    })
    write_json(MODEL_DIR / "shield_controller_forming.json", {
        "parent": "sgjatlantis_cityshield:block/shield_controller_base",
    })
    write_json(MODEL_DIR / "shield_controller_active.json", {
        "parent": "sgjatlantis_cityshield:block/shield_controller_base",
    })
    write_json(MODEL_DIR / "shield_controller_warning.json", {
        "parent": "sgjatlantis_cityshield:block/shield_controller_base",
        "textures": {
            "lightbar": "sgjatlantis_cityshield:block/atlantis_prop_lightbar_warning",
            "projector": "sgjatlantis_cityshield:block/atlantis_shield_projector_warning",
        },
    })
    write_json(MODEL_DIR / "shield_controller.json", {
        "parent": "sgjatlantis_cityshield:block/shield_controller_standby",
    })


if __name__ == "__main__":
    main()
