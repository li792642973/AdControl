package org.ad.annotation;

import java.util.HashMap;
import java.util.Map;

/*
 * 保证不被混淆的位置
 * 1、org.ad.process.imp
 * 2、org.ad.process.macro
 */
public class AdControl {
    private AdControl() { }

    private static final AdControl control = new AdControl();

    public static AdControl getControl() {
        return control;
    }

    private HashMap<String, Class<?>> map = new HashMap<>();

    /**
     * 获取对应广告控制器Class
     *
     * @param key 广告商名称
     * @return 广告商控制器
     */
    public Class getClass(String key) {
        if (map.isEmpty()) {
            synchronized (map) {
                try {
                    Class c = Class.forName("org.ad.process.macro.AdControlMacro");
                    this.map.putAll((Map<String, ? extends Class<?>>) c.getDeclaredField("adControls").get(c));
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return this.map.get(key);
            }
        }

        return this.map.get(key);
    }
}
