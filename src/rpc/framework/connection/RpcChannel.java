package rpc.framework.connection;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketException;
import java.util.Arrays;

import rpc.util.RpcLog;
import rpc.util.RpcTools;



public class RpcChannel implements IRpcChannel{
	private static final String TAG = "RpcChannel";
    private static final boolean DEBUG = true;
    private static final int BUFFER_BLOCK_LEN = 50 * 1024;
    
	Socket mSocket = null;
	OutputStream mSocketOutput = null;
	InputStream mSocketInput = null;

	public RpcChannel(Socket socket) throws IOException {
		mSocket = socket;
		if (mSocket==null || !mSocket.isConnected()) {
			RpcLog.d(TAG, "socket is illegal");
			throw new SocketException("socket is illegal");
		}
		try {
		mSocket.setReceiveBufferSize(BUFFER_BLOCK_LEN);
		mSocket.setSendBufferSize(BUFFER_BLOCK_LEN);
		mSocketOutput = mSocket.getOutputStream();
		mSocketInput = mSocket.getInputStream();
		} catch (IOException e){
			RpcLog.e("TAG", "new RpcChannel failed");
			close();
			throw e;
		}
	}


	@Override
	public void send(byte[] cont) throws IOException {
		synchronized(this){
			if (!mSocket.isConnected()) {
				RpcLog.e(TAG, "Socket is disconneted!");
				throw new IOException("socket is closed");
			} 
		    if (mSocketOutput != null) {
		        mSocketOutput.write(cont);
		        if(DEBUG) RpcLog.d(TAG, "Send request:" + Arrays.toString(cont));
		    } else {
		    	throw new IOException("mSocketOutput is null");
		    }
		}
	}
	

	@Override
	public byte[] recv() throws IOException {
		byte[] readBuffer = new byte[BUFFER_BLOCK_LEN];
		if (!mSocket.isConnected()) {
			RpcLog.e(TAG, "Socket is disconneted!");
			throw new IOException("socket is closed");
		} 
		if (mSocketInput == null) {
			RpcLog.e(TAG, "socket input is null");
			throw new IOException("socketinput is null");
		}
		
		int countRead = mSocketInput.read(readBuffer, 0, BUFFER_BLOCK_LEN);
		if (countRead < 0) {
			RpcLog.e(TAG, "ReceiverThread read disconnected msg:" + countRead);
			throw new IOException("socket is broken");
		} 
		
		byte[] buffer = new byte[countRead];
		System.arraycopy(readBuffer, 0, buffer, 0, countRead); //FIXME:: why need copy again
		if (DEBUG)	RpcLog.d(TAG, "received:" + Arrays.toString(buffer));
		return buffer;
	}
	
	@Override
	public synchronized void close() {
		// 不要重置为null,免得send 和recv时出现空指针错误
		try {
			if (mSocket != null) {
				mSocket.close();
			}
			if (mSocketOutput != null) {
				mSocketOutput.close();
			}
			if (mSocketInput != null) {
				mSocketInput.close();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		RpcLog.d(TAG, "channel is closed");
	}
}
