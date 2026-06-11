import os
import re

manifest_path = r'c:\Coding Files\FluidCheck\app\src\main\AndroidManifest.xml'
manager_path = r'c:\Coding Files\FluidCheck\app\src\main\java\com\example\fluidcheck\util\AppIconManager.kt'

backgrounds = [
    'NONE', 'BLUE', 'DARK', 'ORANGE', 'CYBERPUNK', 'MINIMAL',
    'DEEP_SPACE', 'NORDIC_SLATE', 'WARM_SAND', 'SAGE_GARDEN', 'BURGUNDY',
    'AURORA', 'SUNSET', 'OCEAN', 'GRID', 'STRIPES'
]

foregrounds = [
    'DEFAULT', 'BLUSH_PINK', 'BRONZE', 'CHAPAGNE', 'CREAM_YELLOW', 'CYBER_BLUE', 
    'ELECTRIC_PURPLE', 'GUNMETAL', 'HOT_PINK', 'LAVENDER_BLUE', 'LIME_GREEN', 
    'NEON_ORANGE', 'PEACH_FUZZ', 'PLATINUM_SILVER', 'ROSE_GOLD', 'SAGE_GREEN', 'SOFT_MINT'
]

# 1. Update AndroidManifest.xml
with open(manifest_path, 'r', encoding='utf-8') as f:
    manifest_content = f.read()

# We need to remove all existing activity-aliases
manifest_content = re.sub(r'<activity-alias.*?</activity-alias>', '', manifest_content, flags=re.DOTALL)

alias_template = '''
        <activity-alias
            android:name=".MainActivity_{fg}_{bg}"
            android:targetActivity=".MainActivity"
            android:icon="@mipmap/ic_launcher_{fg}_{bg}"
            android:roundIcon="@mipmap/ic_launcher_{fg}_{bg}"
            android:theme="@style/Theme.App.Starting.{theme_suffix}"
            android:enabled="{enabled}"
            android:exported="true">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity-alias>'''

aliases_xml = ""
for fg in foregrounds:
    for bg in backgrounds:
        is_default = (fg == 'DEFAULT' and bg == 'BLUE')
        enabled = "true" if is_default else "false"
        # Determine theme suffix
        theme_suffix = bg.capitalize()
        if bg == 'DEEP_SPACE':
            theme_suffix = 'Dark'
        elif bg == 'NORDIC_SLATE':
            theme_suffix = 'Dark'
        elif bg == 'WARM_SAND':
            theme_suffix = 'Minimal'
        elif bg == 'SAGE_GARDEN':
            theme_suffix = 'Minimal'
        elif bg == 'BURGUNDY':
            theme_suffix = 'Dark'
        elif bg == 'AURORA':
            theme_suffix = 'Cyberpunk'
        elif bg == 'SUNSET':
            theme_suffix = 'Orange'
        elif bg == 'OCEAN':
            theme_suffix = 'Blue'
        elif bg == 'GRID':
            theme_suffix = 'Cyberpunk'
        elif bg == 'STRIPES':
            theme_suffix = 'Minimal'
            
        aliases_xml += alias_template.format(fg=fg.lower(), bg=bg.lower(), theme_suffix=theme_suffix, enabled=enabled)

# Insert after <activity android:name=".MainActivity" ... </activity>
insert_pos = manifest_content.find('</activity>') + len('</activity>')
new_manifest = manifest_content[:insert_pos] + aliases_xml + manifest_content[insert_pos:]

# Also fix the empty lines created by re.sub
new_manifest = os.linesep.join([s for s in new_manifest.splitlines() if s.strip() or '<' not in s])

with open(manifest_path, 'w', encoding='utf-8') as f:
    f.write(new_manifest)

# 2. Rewrite AppIconManager.kt
manager_code = '''package com.example.fluidcheck.util

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log

object AppIconManager {

    private val allAliases = listOf(
'''
for fg in foregrounds:
    for bg in backgrounds:
        manager_code += f'        "com.example.fluidcheck.MainActivity_{fg.lower()}_{bg.lower()}",\n'

manager_code += '''    )

    fun changeAppIcon(context: Context, iconVariant: String, backgroundVariant: String) {
        val targetAlias = "com.example.fluidcheck.MainActivity_${iconVariant.lowercase()}_${backgroundVariant.lowercase()}"
        val pm = context.packageManager

        // 1. Disable all other aliases FIRST using DONT_KILL_APP
        for (alias in allAliases) {
            if (alias != targetAlias) {
                try {
                    val state = pm.getComponentEnabledSetting(ComponentName(context, alias))
                    if (state != PackageManager.COMPONENT_ENABLED_STATE_DISABLED) {
                        pm.setComponentEnabledSetting(
                            ComponentName(context, alias),
                            PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                            PackageManager.DONT_KILL_APP
                        )
                    }
                } catch (e: Exception) {
                    // Ignore
                }
            }
        }

        // 2. Enable the target alias LAST and kill the app
        try {
            pm.setComponentEnabledSetting(
                ComponentName(context, targetAlias),
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                0 // Kill app to apply changes and refresh launcher
            )
        } catch (e: Exception) {
            Log.e("AppIconManager", "Failed to enable alias $targetAlias", e)
        }
    }
}
'''

with open(manager_path, 'w', encoding='utf-8') as f:
    f.write(manager_code)
