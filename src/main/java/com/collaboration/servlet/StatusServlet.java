package com.collaboration.servlet;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.io.PrintWriter;

/**
 * Native Java Servlet implementation.
 * Showcases Advanced Java Servlet integration alongside Spring Boot.
 */
@WebServlet(name = "StatusServlet", urlPatterns = "/api/status")
public class StatusServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");
        
        PrintWriter out = resp.getWriter();
        out.print("{\"status\":\"online\", \"module\":\"Manual HttpServlet\", \"message\":\"CodeCollab Engine is fully operational!\"}");
        out.flush();
    }
}
