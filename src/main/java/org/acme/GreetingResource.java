package org.acme;

import com.esotericsoftware.kryo.serializers.TimeSerializers;
import jakarta.annotation.PostConstruct;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jdk.jfr.consumer.RecordingStream;

import java.util.concurrent.Executors;

@Path("/hello")
public class GreetingResource {

    @PostConstruct
    public void init() {


    }

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String hello() {

        RecordingStream eventStream = new RecordingStream();
        eventStream.enable("jdk.VirtualThreadPinned").withStackTrace();
        eventStream.onEvent("jdk.VirtualThreadPinned", x -> {

            System.out.println("JFR stack frames:");
            x.getStackTrace()
                    .getFrames()
                    .forEach(y -> System.out.println(y));

            System.err.println(x);
        });
        eventStream.startAsync();


        var cl = Thread.currentThread()
                .getContextClassLoader();
        System.out.println(cl.getClass());

        var runnable = new Runnable() {
            @Override
            public void run() {
                try {
                    cl.loadClass(TimeSerializers.InstantSerializer.class.getName());
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };

        try (var exec = Executors.newVirtualThreadPerTaskExecutor()) {
            for (int i = 0; i < 1000; i++) {
                exec.submit(runnable);
            }
        }
        return "Hello from Quarkus REST";
    }
}
