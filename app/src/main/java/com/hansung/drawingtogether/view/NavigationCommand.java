package com.hansung.drawingtogether.view;

import android.os.Bundle;

public abstract class NavigationCommand {
    private NavigationCommand() {}

    public final static class To extends NavigationCommand {
        private final int destinationId;

        public To(int destinationId) {
            this.destinationId = destinationId;
        }

        public int getDestinationId() {
            return destinationId;
        }
    }

    public final static class Back extends NavigationCommand {

    }

    public final static class ToBundle extends NavigationCommand {
        private final int destinationId;
        private final Bundle bundle;

        public ToBundle(int destinationId, Bundle bundle) {
            this.destinationId = destinationId;
            this.bundle = bundle;
        }

        public int getDestinationId() {
            return destinationId;
        }

        public Bundle getBundle() {
            return bundle;
        }
    }
}
