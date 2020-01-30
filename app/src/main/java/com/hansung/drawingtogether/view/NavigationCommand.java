package com.hansung.drawingtogether.view;

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
}
