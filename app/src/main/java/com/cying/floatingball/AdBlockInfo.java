package com.cying.floatingball;

import android.support.annotation.Keep;

import com.cying.lightorm.Column;
import com.cying.lightorm.Key;
import com.cying.lightorm.Table;

/**
 * Created by Cying on 17/11/9.
 */
@Table("ad_block")
@Keep
public class AdBlockInfo {

    @Key
    public Long id;

    @Column(unique = true, notNull = true)
    public String windowClassName;

    @Column
    public String appName;

    @Column
    public String packageName;

    @Column
    public String buttonClassName;

    @Column
    public float buttonArea;

    @Column
    public String buttonText;

    @Column
    public String buttonContentDesc;

    @Column
    public Boolean buttonClickable;

    @Override
    public String toString() {
        return appName + "：" + windowClassName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        AdBlockInfo that = (AdBlockInfo) o;

        return windowClassName.equals(that.windowClassName);

    }

    @Override
    public int hashCode() {
        return windowClassName.hashCode();
    }
}
