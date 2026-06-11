import os
import glob

# Paths
res_dir = r'c:\Coding Files\FluidCheck\app\src\main\res'
drawable_dir = os.path.join(res_dir, 'drawable')
mipmap_dir = os.path.join(res_dir, 'mipmap-anydpi-v26')

os.makedirs(drawable_dir, exist_ok=True)
os.makedirs(mipmap_dir, exist_ok=True)

# 1. Delete old XML foreground files to clean up
for old_xml in glob.glob(os.path.join(drawable_dir, 'ic_launcher_fg_*.xml')):
    try:
        os.remove(old_xml)
    except Exception:
        pass

# Delete old mipmap XML configurations to avoid stale resource linking failures
for old_xml in glob.glob(os.path.join(mipmap_dir, 'ic_launcher_*.xml')):
    filename = os.path.basename(old_xml)
    if filename not in ['ic_launcher.xml', 'ic_launcher_round.xml']:
        try:
            os.remove(old_xml)
        except Exception:
            pass

# Define backgrounds
solid_backgrounds = {
    'NONE': '#00000000',
    'BLUE': '#F0F9FF',
    'DARK': '#1E1E1E',
    'ORANGE': '#FFEDD5',
    'CYBERPUNK': '#0B0F19',
    'MINIMAL': '#F8FAFC',
    'DEEP_SPACE': '#0A0F1D',
    'NORDIC_SLATE': '#2E3440',
    'WARM_SAND': '#FAF8F5',
    'SAGE_GARDEN': '#E8EFE9',
    'BURGUNDY': '#4C0519'
}

# 2. Generate Solid Background Drawables
bg_solid_template = '''<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="108dp"
    android:height="108dp"
    android:viewportWidth="108"
    android:viewportHeight="108">
    <path
        android:fillColor="{color}"
        android:pathData="M0,0h108v108h-108z" />
</vector>'''

for name, color in solid_backgrounds.items():
    with open(os.path.join(drawable_dir, f'ic_launcher_bg_{name.lower()}.xml'), 'w') as f:
        f.write(bg_solid_template.format(color=color))

# 3. Generate Gradient Background Drawables
gradients = {
    'AURORA': '''<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:aapt="http://schemas.android.com/aapt"
    android:width="108dp"
    android:height="108dp"
    android:viewportWidth="108"
    android:viewportHeight="108">
    <path android:pathData="M0,0h108v108h-108z">
        <aapt:attr name="android:fillColor">
            <gradient
                android:startX="0"
                android:startY="0"
                android:endX="108"
                android:endY="108"
                android:type="linear">
                <item android:offset="0.0" android:color="#0F172A" />
                <item android:offset="0.5" android:color="#1E1B4B" />
                <item android:offset="1.0" android:color="#311042" />
            </gradient>
        </aapt:attr>
    </path>
</vector>''',
    'SUNSET': '''<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:aapt="http://schemas.android.com/aapt"
    android:width="108dp"
    android:height="108dp"
    android:viewportWidth="108"
    android:viewportHeight="108">
    <path android:pathData="M0,0h108v108h-108z">
        <aapt:attr name="android:fillColor">
            <gradient
                android:startX="54"
                android:startY="0"
                android:endX="54"
                android:endY="108"
                android:type="linear">
                <item android:offset="0.0" android:color="#FF7E5F" />
                <item android:offset="1.0" android:color="#FEB47B" />
            </gradient>
        </aapt:attr>
    </path>
</vector>''',
    'OCEAN': '''<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:aapt="http://schemas.android.com/aapt"
    android:width="108dp"
    android:height="108dp"
    android:viewportWidth="108"
    android:viewportHeight="108">
    <path android:pathData="M0,0h108v108h-108z">
        <aapt:attr name="android:fillColor">
            <gradient
                android:startX="0"
                android:startY="108"
                android:endX="108"
                android:endY="0"
                android:type="linear">
                <item android:offset="0.0" android:color="#0052D4" />
                <item android:offset="0.5" android:color="#4364F7" />
                <item android:offset="1.0" android:color="#6FB1FC" />
            </gradient>
        </aapt:attr>
    </path>
</vector>'''
}

for name, xml_content in gradients.items():
    with open(os.path.join(drawable_dir, f'ic_launcher_bg_{name.lower()}.xml'), 'w') as f:
        f.write(xml_content.strip())

# 4. Generate Patterned Background Drawables
patterns = {
    'GRID': '''<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="108dp"
    android:height="108dp"
    android:viewportWidth="108"
    android:viewportHeight="108">
    <path
        android:fillColor="#121824"
        android:pathData="M0,0h108v108h-108z" />
    <path
        android:fillColor="#1F293D"
        android:pathData="M18,18m-2,0a2,2 0,1 1,4 0a2,2 0,1 1,-4 0 M36,18m-2,0a2,2 0,1 1,4 0a2,2 0,1 1,-4 0 M54,18m-2,0a2,2 0,1 1,4 0a2,2 0,1 1,-4 0 M72,18m-2,0a2,2 0,1 1,4 0a2,2 0,1 1,-4 0 M90,18m-2,0a2,2 0,1 1,4 0a2,2 0,1 1,-4 0 M18,36m-2,0a2,2 0,1 1,4 0a2,2 0,1 1,-4 0 M36,36m-2,0a2,2 0,1 1,4 0a2,2 0,1 1,-4 0 M54,36m-2,0a2,2 0,1 1,4 0a2,2 0,1 1,-4 0 M72,36m-2,0a2,2 0,1 1,4 0a2,2 0,1 1,-4 0 M90,36m-2,0a2,2 0,1 1,4 0a2,2 0,1 1,-4 0 M18,54m-2,0a2,2 0,1 1,4 0a2,2 0,1 1,-4 0 M36,54m-2,0a2,2 0,1 1,4 0a2,2 0,1 1,-4 0 M54,54m-2,0a2,2 0,1 1,4 0a2,2 0,1 1,-4 0 M72,54m-2,0a2,2 0,1 1,4 0a2,2 0,1 1,-4 0 M90,54m-2,0a2,2 0,1 1,4 0a2,2 0,1 1,-4 0 M18,72m-2,0a2,2 0,1 1,4 0a2,2 0,1 1,-4 0 M36,72m-2,0a2,2 0,1 1,4 0a2,2 0,1 1,-4 0 M54,72m-2,0a2,2 0,1 1,4 0a2,2 0,1 1,-4 0 M72,72m-2,0a2,2 0,1 1,4 0a2,2 0,1 1,-4 0 M90,72m-2,0a2,2 0,1 1,4 0a2,2 0,1 1,-4 0 M18,90m-2,0a2,2 0,1 1,4 0a2,2 0,1 1,-4 0 M36,90m-2,0a2,2 0,1 1,4 0a2,2 0,1 1,-4 0 M54,90m-2,0a2,2 0,1 1,4 0a2,2 0,1 1,-4 0 M72,90m-2,0a2,2 0,1 1,4 0a2,2 0,1 1,-4 0 M90,90m-2,0a2,2 0,1 1,4 0a2,2 0,1 1,-4 0" />
</vector>''',
    'STRIPES': '''<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="108dp"
    android:height="108dp"
    android:viewportWidth="108"
    android:viewportHeight="108">
    <path
        android:fillColor="#F8FAFC"
        android:pathData="M0,0h108v108h-108z" />
    <path
        android:strokeColor="#E2E8F0"
        android:strokeWidth="4"
        android:pathData="M-20,20 L20,-20 M-20,50 L50,-20 M-20,80 L80,-20 M-20,110 L110,-20 M10,130 L130,10 M40,130 L130,40 M70,130 L130,70" />
</vector>'''
}

for name, xml_content in patterns.items():
    with open(os.path.join(drawable_dir, f'ic_launcher_bg_{name.lower()}.xml'), 'w') as f:
        f.write(xml_content.strip())

# Define new PNG icons
foregrounds = [
    'default', 'blush_pink', 'bronze', 'chapagne', 'cream_yellow', 'cyber_blue', 
    'electric_purple', 'gunmetal', 'hot_pink', 'lavender_blue', 'lime_green', 
    'neon_orange', 'peach_fuzz', 'platinum_silver', 'rose_gold', 'sage_green', 'soft_mint'
]
backgrounds = list(solid_backgrounds.keys()) + list(gradients.keys()) + list(patterns.keys())

# 5. Generate Adaptive Icons using background XMLs and PNG icon drawables
adaptive_template = '''<?xml version="1.0" encoding="utf-8"?>
<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">
    <background android:drawable="@drawable/ic_launcher_bg_{bg}" />
    <foreground>
        <inset android:drawable="@drawable/{fg}_icon" android:inset="18%" />
    </foreground>
</adaptive-icon>'''

for fg in foregrounds:
    for bg in backgrounds:
        with open(os.path.join(mipmap_dir, f'ic_launcher_{fg.lower()}_{bg.lower()}.xml'), 'w') as f:
            f.write(adaptive_template.format(fg=fg.lower(), bg=bg.lower()))
