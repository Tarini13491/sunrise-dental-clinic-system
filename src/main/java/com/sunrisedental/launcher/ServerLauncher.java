package com.sunrisedental.launcher;

import com.sunrisedental.config.AppConfig;
import com.sunrisedental.config.DatabaseBootstrap;
import com.sunrisedental.controller.AppointmentServlet;
import com.sunrisedental.controller.BillServlet;
import com.sunrisedental.controller.DashboardServlet;
import com.sunrisedental.controller.HealthServlet;
import com.sunrisedental.controller.HelpServlet;
import com.sunrisedental.controller.LoginServlet;
import com.sunrisedental.controller.LogoutServlet;
import com.sunrisedental.controller.PatientServlet;
import com.sunrisedental.controller.ReportServlet;
import com.sunrisedental.controller.SearchServlet;
import com.sunrisedental.controller.SessionServlet;
import com.sunrisedental.controller.StaffServlet;
import com.sunrisedental.filter.AuthenticationFilter;
import com.sunrisedental.filter.CharacterEncodingFilter;
import jakarta.servlet.DispatcherType;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.session.SessionHandler;
import org.eclipse.jetty.servlet.DefaultServlet;
import org.eclipse.jetty.servlet.FilterHolder;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.resource.PathResource;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumSet;

public final class ServerLauncher {

    public static void main(String[] args) throws Exception {
        DatabaseBootstrap.run();

        int port = AppConfig.getInt("server.port", 45321);
        String host = AppConfig.get("server.host", "0.0.0.0");

        Server server = new Server();
        ServerConnector connector = new ServerConnector(server);
        connector.setHost(host);
        connector.setPort(port);
        server.addConnector(connector);

        Path webRoot = resolveWebRoot();
        ServletContextHandler ctx = new ServletContextHandler(ServletContextHandler.SESSIONS);
        ctx.setContextPath("/");
        ctx.setBaseResource(new PathResource(webRoot));
        ctx.setWelcomeFiles(new String[]{"index.html"});

        SessionHandler sessions = ctx.getSessionHandler();
        sessions.setHttpOnly(true);
        sessions.setMaxInactiveInterval(AppConfig.getInt("session.timeout.minutes", 30) * 60);
        sessions.getSessionCookieConfig().setName("SDCSESSION");
        sessions.getSessionCookieConfig().setPath("/");

        ctx.addFilter(new FilterHolder(new CharacterEncodingFilter()), "/*",
                EnumSet.of(DispatcherType.REQUEST));
        ctx.addFilter(new FilterHolder(new AuthenticationFilter()), "/api/*",
                EnumSet.of(DispatcherType.REQUEST));

        ctx.addServlet(LoginServlet.class, "/api/login");
        ctx.addServlet(LogoutServlet.class, "/api/logout");
        ctx.addServlet(SessionServlet.class, "/api/session");
        ctx.addServlet(AppointmentServlet.class, "/api/appointments");
        ctx.addServlet(SearchServlet.class, "/api/search");
        ctx.addServlet(BillServlet.class, "/api/bills");
        ctx.addServlet(DashboardServlet.class, "/api/dashboard");
        ctx.addServlet(ReportServlet.class, "/api/reports");
        ctx.addServlet(HelpServlet.class, "/api/help");
        ctx.addServlet(StaffServlet.class, "/api/staff");
        ctx.addServlet(PatientServlet.class, "/api/patients");
        ctx.addServlet(HealthServlet.class, "/api/health");

        ServletHolder defaults = new ServletHolder("default", DefaultServlet.class);
        defaults.setInitParameter("dirAllowed", "false");
        defaults.setInitParameter("welcomeServlets", "true");
        ctx.addServlet(defaults, "/");

        server.setHandler(ctx);
        server.start();
        System.out.println();
        System.out.println("Sunrise Dental Clinic is running at http://127.0.0.1:" + port + "/");
        System.out.println("Staff portal: http://127.0.0.1:" + port + "/pages/login.html");
        System.out.println("Demo logins: admin / Admin@123  ·  staff / Staff@123");
        System.out.println();
        server.join();
    }

    private static Path resolveWebRoot() {
        Path local = Path.of("src/main/webapp").toAbsolutePath();
        if (Files.isDirectory(local)) {
            return local;
        }
        throw new IllegalStateException("Cannot find src/main/webapp. Run the launcher from the project root.");
    }
}