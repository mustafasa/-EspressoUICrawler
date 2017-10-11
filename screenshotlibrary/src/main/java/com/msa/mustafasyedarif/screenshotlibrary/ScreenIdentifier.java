package com.msa.mustafasyedarif.screenshotlibrary;

/**
 * Created by Mustafa Arif on 10/10/2017.
 */

/**
 * ScreenIdentifier class is utilized to create meta-data information of current UI element on
 * screen.Identifier are set by {@link MSA}.
 */
public class ScreenIdentifier {
    private String uiType;
    private String uiIdentifier;
    private String uiText;
    private int uiHeight;
    private int uiWidth;
    private int uiXaxis;
    private int uiYaxis;
    private boolean uiVisibility;

    /**
     * @return String current UI element type.
     */
    public String getUiType() {
        return uiType;
    }

    protected void setUiType(String uiType) {
        this.uiType = uiType;
    }

    /**
     * @return String current UI element Identifier.
     */
    public String getUiIdentifier() {
        return uiIdentifier;
    }

    protected void setUiIdentifier(String uiIdentifier) {
        this.uiIdentifier = uiIdentifier;
    }

    /**
     * @return String current UI element Text.
     */
    public String getUiText() {
        return uiText;
    }

    protected void setUiText(String uiText) {
        this.uiText = uiText;
    }

    /**
     * @return String current UI element Height.
     */
    public int getUiHeight() {
        return uiHeight;
    }

    protected void setUiHeight(int uiHeight) {
        this.uiHeight = uiHeight;
    }

    /**
     * @return String current UI element Width.
     */
    public int getUiWidth() {
        return uiWidth;
    }

    protected void setUiWidth(int uiWidth) {
        this.uiWidth = uiWidth;
    }

    /**
     * @return String current UI element X-axis.
     */
    public int getUiXaxis() {
        return uiXaxis;
    }

    protected void setUiXaxis(int uiXaxis) {
        this.uiXaxis = uiXaxis;
    }

    /**
     * @return String current UI element Y-axis.
     */
    public int getUiYaxis() {
        return uiYaxis;
    }

    protected void setUiYaxis(int uiYaxis) {
        this.uiYaxis = uiYaxis;
    }

    /**
     * @return <code>true</code> if UI element visible, <code>false</code>
     * otherwise.
     */
    public boolean isUiVisibile() {
        return uiVisibility;
    }

    protected void setUiVisibility(boolean uiVisibility) {
        this.uiVisibility = uiVisibility;
    }

}
