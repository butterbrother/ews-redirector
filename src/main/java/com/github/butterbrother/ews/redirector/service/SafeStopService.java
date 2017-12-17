package com.github.butterbrother.ews.redirector.service;

/**
 * Сервис с плавной остановкой
 */
public abstract class SafeStopService
        implements Runnable {
    private boolean active = true;
    private boolean done = false;
    private Thread currentThread = null;

    /**
     * Прекращает активность сервиса
     */
    public synchronized void safeStop() {
        active = false;
        if (currentThread != null)
            currentThread.interrupt();
    }

    /**
     * Проверка активности сервиса
     *
     * @return текущий статус
     */
    public synchronized boolean isActive() {
        return active;
    }

    /**
     * Проверка завершения работы сервиса
     *
     * @return текущий статус
     */
    public synchronized boolean isDone() {
        return done;
    }

    /**
     * Установка флага завершения работы сервиса
     * Вызов выполняется по завершению работы потока.
     */
    void wellDone() {
        done = true;
        active = false;
    }

    /**
     * Запуск сервиса
     */
    void runService() {
        currentThread = new Thread(this);
        currentThread.start();
    }

    public void join() throws InterruptedException {
        if (currentThread != null)
            currentThread.join();
    }
}
