package async;

public class EchoLineReadWriteHandlerFactory implements ISocketReadWriteHandlerFactory {
	public IReadWriteHandler createHandler() {
		return new RequestHandler();
		// return new EchoLineReadWriteHandler();
	}
}
