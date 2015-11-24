/**
 * 
 */
package com.cdc.ws.controller;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author Selva
 * DemoController is used for development purpose.
 */
@RestController
public class DemoController {

	@RequestMapping(value = "/helloworld")
	public String helloworld() {
		System.out.println("helloworld");
		return "\"Hello World\"";
	}
	
	@RequestMapping(value = "/sum/{a}/{b}")
	public String sum(@PathVariable("a") int a, @PathVariable("b") int b) {
		System.out.println("sum");
		return String.valueOf(a+b);
	}
}
