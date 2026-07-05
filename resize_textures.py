#!/usr/bin/env python3
"""缩放材质到16x16像素"""

from PIL import Image
from pathlib import Path

TEXTURES_DIR = Path("src/main/resources/assets/templenihility/textures")

def resize_texture(path: Path):
    """缩放单个材质到16x16"""
    try:
        img = Image.open(path)
        img = img.resize((16, 16), Image.NEAREST)
        img.save(path)
        print(f"缩放: {path.name}")
    except Exception as e:
        print(f"失败: {path.name} - {e}")

def main():
    """主函数"""
    print("=== 缩放材质到16x16 ===\n")

    count = 0
    for png_file in TEXTURES_DIR.rglob("*.png"):
        resize_texture(png_file)
        count += 1

    print(f"\n完成! 共处理 {count} 个文件")

if __name__ == "__main__":
    main()
