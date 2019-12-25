package com.panzhanpeng.service.Impl;

import com.panzhanpeng.annotation.SpringService;
import com.panzhanpeng.service.TestService;

@SpringService
public class TestServiceImpl implements TestService {

	public String getNameAndAge(String name, String age) {
		return name + age;
	}

}
