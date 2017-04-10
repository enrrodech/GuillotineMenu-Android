package com.yalantis.guillotine.interfaces;

/**
 * Created by Dmytro Denysenko on 5/6/15.
 */
public interface GuillotineListener {
    void onGuillotineWillOpen();
    void onGuillotineWillClose();
    void onGuillotineOpened();
    void onGuillotineClosed();
}
