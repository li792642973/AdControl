package org.ad.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
public @interface Adc {
    // 广告商名称
    String name();

    // 是否是SDK (默认是true，只有在API广告商才设置为false)
    boolean sdk() default true;
}
