package com.hansung.drawingtogether.view.drawing;

import android.view.View;

public abstract class DrawingCommand {
    private DrawingCommand() {}

    public final static class PenMode extends DrawingCommand {
        private final View view;

        public PenMode(View view) {
            this.view = view;
        }

        public View getView() {
            return view;
        }
    }

    public final static class PenSetting extends DrawingCommand {
        private final View view;

        public PenSetting(View view) {
            this.view = view;
        }

        public View getView() {
            return view;
        }
    }

    public final static class EraserMode extends DrawingCommand {
        private final View view;

        public EraserMode(View view) {
            this.view = view;
        }

        public View getView() {
            return view;
        }
    }

    public final static class Undo extends DrawingCommand { }
    public final static class Redo extends DrawingCommand { }

    public final static class TextMode extends DrawingCommand {
        private final View view;

        public TextMode(View view) {
            this.view = view;
        }

        public View getView() {
            return view;
        }
    }

    public final static class ShapeMode extends DrawingCommand {
        private final View view;

        public ShapeMode(View view) {
            this.view = view;
        }

        public View getView() {
            return view;
        }
    }
}
