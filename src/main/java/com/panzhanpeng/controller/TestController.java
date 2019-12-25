package com.panzhanpeng.controller;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.panzhanpeng.annotation.SpringAutoWried;
import com.panzhanpeng.annotation.SpringController;
import com.panzhanpeng.annotation.SpringRequestMapping;
import com.panzhanpeng.annotation.SpringRequestParam;
import com.panzhanpeng.service.TestService;

@SpringController
@SpringRequestMapping("/testController")
public class TestController {
	
	@SpringAutoWried
	private TestService testService;

	@SpringRequestMapping("/get")
	public void getNameAndAge(HttpServletRequest request,HttpServletResponse response, @SpringRequestParam("name") String name, @SpringRequestParam("age") String age) {
		String result = testService.getNameAndAge(name, age);
		try {
			PrintWriter pw = response.getWriter();
			pw.print(result);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
