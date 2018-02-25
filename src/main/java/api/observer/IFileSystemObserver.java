package api.observer;

public interface IFileSystemObserver extends IObserver {
    @Override
    void updateState();
}
