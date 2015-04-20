package jazmin.server.rtmp;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;

import jazmin.core.Jazmin;
import jazmin.core.Server;
import jazmin.misc.InfoBuilder;
import jazmin.server.console.ConsoleServer;
import jazmin.server.rtmp.rtmp.server.RtmpConfig;
import jazmin.server.rtmp.rtmp.server.ServerApplication;
import jazmin.server.rtmp.rtmp.server.ServerHandler;
import jazmin.server.rtmp.rtmp.server.ServerPipelineFactory;
import jazmin.server.rtmp.util.Utils;

import org.jboss.netty.bootstrap.ServerBootstrap;
import org.jboss.netty.channel.ChannelFactory;
import org.jboss.netty.channel.socket.nio.NioServerSocketChannelFactory;
/**
 * 
 *
 */
public class RtmpServer extends Server{
	private static Map<String, ServerApplication> applications;
    private static ChannelFactory factory;
    private static List<ServerHandler>handlers;
	public RtmpServer() {
		applications = new ConcurrentHashMap<String, ServerApplication>();
	    handlers=Collections.synchronizedList(new ArrayList<>());
	}
	//--------------------------------------------------------------------------
	//
	public void setPort(int port){
		if(isStarted()){
			throw new IllegalStateException("set before started");
		}
		RtmpConfig.serverPort=port;
	}
	//
	public void setServerHome(String home){
		if(isStarted()){
			throw new IllegalStateException("set before started");
		}
		RtmpConfig.homeDir=home;
	}
	//
	public String getServerHome(){
		return RtmpConfig.homeDir;
	}
	//
	public int getPort(){
		return RtmpConfig.serverPort;
	}
	//
	public List<ServerApplication>getApplications(){
		return new ArrayList<ServerApplication>(applications.values());
	}
	//
	public List<ServerHandler>getHandlers(){
		return new ArrayList<>(handlers);
	}
	//--------------------------------------------------------------------------
	public static void addHandler(ServerHandler handler){
		handlers.add(handler);
	}
	//
	public static void removeHandler(ServerHandler handler){
		handlers.remove(handler);
	}
	//--------------------------------------------------------------------------
	@Override
	public void init() throws Exception {
		ConsoleServer cs=Jazmin.getServer(ConsoleServer.class);
		if(cs!=null){
			cs.registerCommand(new RtmpServerCommand());
		}
	}
	@Override
    public void start() throws Exception {
        factory = new NioServerSocketChannelFactory(
                Executors.newCachedThreadPool(),
                Executors.newCachedThreadPool());
        final ServerBootstrap bootstrap = new ServerBootstrap(factory);
        bootstrap.setPipelineFactory(new ServerPipelineFactory());
        bootstrap.setOption("child.tcpNoDelay", true);
        bootstrap.setOption("child.keepAlive", true);
        final InetSocketAddress socketAddress = new InetSocketAddress(RtmpConfig.serverPort);
        bootstrap.bind(socketAddress);
	}
	//
	public static ServerApplication getApplication(final String rawName) {
		final String appName = Utils.trimSlashes(rawName).toLowerCase();
		ServerApplication app = RtmpServer.applications.get(appName);
		if (app == null) {
			app = new ServerApplication(appName);
			RtmpServer.applications.put(appName, app);
		}
		return app;
	}
    //
	@Override
    public void stop(){
        factory.releaseExternalResources();
    }
    //
    @Override
    public String info() {
    	InfoBuilder ib=InfoBuilder.create();
		ib.section("info")
		.format("%-30s:%-30s\n")
		.print("port",getPort())
		.print("serverHome",getServerHome());
		return ib.toString();
    }
}
