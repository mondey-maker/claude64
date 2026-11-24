package com.claude64.app;

import org.libsdl.app.SDLActivity;

/** Runs the native game (libmain.so, built from sm64coopdx) inside SDL. Launch args come from the menu. */
public class GameActivity extends SDLActivity {
    public static final String EXTRA_ARGS = "args";

    @Override
    protected String[] getLibraries() {
        return new String[] { "SDL2", "main" };
    }

    @Override
    protected String[] getArguments() {
        String[] a = getIntent() != null ? getIntent().getStringArrayExtra(EXTRA_ARGS) : null;
        return a != null ? a : new String[0];
    }
}
