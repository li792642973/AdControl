package org.test.adcontrol;

import org.ad.annotation.Adc;

@Adc(name = "XAd3", sdk = false)
public class Ad3Control {
    public String ua = "";

    public void getUa1() {

    }

    public String getUa() {
        return this.ua;
    }
}
