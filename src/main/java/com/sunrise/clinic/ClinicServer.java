package com.sunrise.clinic;

import com.sunrise.clinic.config.AppSettings;
import com.sunrise.clinic.config.SchemaBootstrap;
import com.sunrise.clinic.web.AppointmentServlet;
import com.sunrise.clinic.web.AuthFilter;
import com.sunrise.clinic.web.AuthServlet;
import com.sunrise.clinic.web.BillingServlet;
import com.sunrise.clinic.web.DentistServlet;
import com.sunrise.clinic.web.PatientServlet;
import com.sunrise.clinic.web.ReportServlet;
import com.sunrise.clinic.web.StaffServlet;
import com.sunrise.clinic.web.TreatmentServlet;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.DefaultServlet;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;

import jakarta.servlet.DispatcherType;
import java.net.InetSocketAddress;
import java.nio.file.Path;
import java.util.EnumSet;

public final class ClinicServer {
    public static void main(String[] args) throws Exception {
        SchemaBootstrap.prepare();
        AppSettings settings = AppSettings.INSTANCE;
        Server server = new Server(new InetSocketAddress(settings.serverHost(), settings.serverPort()));

        ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
        context.setContextPath("/");
        Path webDir = Path.of("src/main/webapp").toAbsolutePath();
        context.setResourceBase(webDir.toString());
        context.setWelcomeFiles(new String[]{"index.html"});
        context.getSessionHandler().setMaxInactiveInterval(settings.sessionTimeoutMinutes() * 60);

        context.addFilter(AuthFilter.class, "/api/*", EnumSet.of(DispatcherType.REQUEST));
        context.addFilter(AuthFilter.class, "/desk.html", EnumSet.of(DispatcherType.REQUEST));

        map(context, new AuthServlet(), "/api/auth/*");
        map(context, new StaffServlet(), "/api/staff", "/api/staff/*");
        map(context, new PatientServlet(), "/api/patients", "/api/patients/*");
        map(context, new AppointmentServlet(), "/api/appointments", "/api/appointments/*");
        map(context, new BillingServlet(), "/api/bills", "/api/bills/*");
        map(context, new ReportServlet(), "/api/reports", "/api/reports/*");
        map(context, new TreatmentServlet(), "/api/treatments");
        map(context, new DentistServlet(), "/api/dentists", "/api/dentists/*");

        ServletHolder staticFiles = new ServletHolder("default", DefaultServlet.class);
        staticFiles.setInitParameter("dirAllowed", "false");
        context.addServlet(staticFiles, "/");

        server.setHandler(context);
        server.start();
        System.out.println("Sunrise Dental Clinic is running at http://localhost:" + settings.serverPort());
        server.join();
    }

    private static void map(ServletContextHandler context, jakarta.servlet.Servlet servlet, String... paths) {
        ServletHolder holder = new ServletHolder(servlet);
        for (String path : paths) {
            context.addServlet(holder, path);
        }
    }
}
