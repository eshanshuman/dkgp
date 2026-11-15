package com.campusexchange.ui;

import javax.swing.JOptionPane;
import javax.swing.SwingWorker;
import java.awt.Component;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;

/**
 * Runs database work off the EDT with a {@link SwingWorker}, so the window never freezes.
 *
 * <p>Usage:
 * <pre>
 * DbTask.run(this, () -> resourceDAO.findAll(), list -> showInTable(list));
 * </pre>
 * The background part runs on a worker thread; the {@code onSuccess} callback runs back on
 * the EDT. Any exception (including every SQLException) is shown in a JOptionPane.
 */
public final class DbTask {

    /** Database work that returns a value and may throw (e.g. SQLException). */
    @FunctionalInterface
    public interface Work<T> {
        T call() throws Exception;
    }

    private DbTask() {
    }

    public static <T> void run(Component parent, Work<T> work, Consumer<T> onSuccess) {
        run(parent, work, onSuccess, () -> { });
    }

    /**
     * Same as {@link #run(Component, Work, Consumer)}, plus {@code always}, which runs on the
     * EDT when the work finishes - whether it succeeded or failed - before anything else.
     * Handy for re-enabling buttons.
     */
    public static <T> void run(Component parent, Work<T> work, Consumer<T> onSuccess, Runnable always) {
        new SwingWorker<T, Void>() {
            @Override
            protected T doInBackground() throws Exception {
                return work.call();
            }

            @Override
            protected void done() {
                always.run();
                try {
                    onSuccess.accept(get());
                } catch (ExecutionException e) {
                    showError(parent, e.getCause());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }.execute();
    }

    /** Shows an error message dialog. Must be called on the EDT. */
    public static void showError(Component parent, Throwable error) {
        String message = error.getMessage() != null ? error.getMessage() : error.toString();
        JOptionPane.showMessageDialog(parent, message, "Database error", JOptionPane.ERROR_MESSAGE);
    }
}
