package com.carrotgarden.maven.bintray;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class HttpDemo {

	static void log(String text) {
		System.out.println(text);
	}

	static String run(String url) throws Exception {
		Request request = new Request.Builder().url(url).build();
		Response response = client.newCall(request).execute();
		log("code=" + response.code());
		log("mesg=" + response.message());
		return response.body().string();
	}

	static OkHttpClient client = new OkHttpClient();

	public static void main(String[] args) throws Exception {

		log(run("https://api.bintray.com/packages/random-maven/maven/pack"));

	}

}
