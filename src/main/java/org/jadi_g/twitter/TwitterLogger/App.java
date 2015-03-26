//
// Author:: Grégoire Jadi <daimrod@gmail.com>
// Copyright:: Copyright (c) 2015, Grégoire Jadi
// All rights reserved.
//
// Redistribution and use in source and binary forms, with or without
// modification, are permitted provided that the following conditions are
// met:
//
//    1. Redistributions of source code must retain the above copyright
//       notice, this list of conditions and the following disclaimer.
//
//    2. Redistributions in binary form must reproduce the above
//       copyright notice, this list of conditions and the following
//       disclaimer in the documentation and/or other materials provided
//       with the distribution.
//
// THIS SOFTWARE IS PROVIDED BY GRÉGOIRE JADI ``AS IS'' AND ANY
// EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
// IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
// PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL GRÉGOIRE JADI OR
// CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
// SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
// LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
// USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
// ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
// OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
// OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
// SUCH DAMAGE.
//
// The views and conclusions contained in the software and
// documentation are those of the authors and should not be
// interpreted as representing official policies, either expressed or
// implied, of Grégoire Jadi.
//

package org.jadi_g.twitter.TwitterLogger;

import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.apache.commons.collections4.queue.CircularFifoQueue;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;
import com.twitter.hbc.ClientBuilder;
import com.twitter.hbc.core.Client;
import com.twitter.hbc.core.Constants;
import com.twitter.hbc.core.Hosts;
import com.twitter.hbc.core.HttpHosts;
import com.twitter.hbc.core.endpoint.StatusesFilterEndpoint;
import com.twitter.hbc.core.processor.StringDelimitedProcessor;
import com.twitter.hbc.httpclient.auth.Authentication;
import com.twitter.hbc.httpclient.auth.OAuth1;

public class App {
	public static void main(String[] args) throws Exception {
        Logger logger = LoggerFactory.getLogger("TwitterLogger");


        String configFileName = "access.conf";
		PropertiesConfiguration config = new PropertiesConfiguration(
				configFileName);

		/**
		 * Set up your blocking queues: Be sure to size these properly based on
		 * expected TPS of your stream
		 */
		BlockingQueue<String> msgQueue = new LinkedBlockingQueue<String>(100000);

		/**
		 * Declare the host you want to connect to, the endpoint, and
		 * authentication (basic auth or oauth)
		 */
		Hosts hosebirdHosts = new HttpHosts(Constants.STREAM_HOST);
		StatusesFilterEndpoint hosebirdEndpoint = new StatusesFilterEndpoint();
		// Optional: set up some followings and track terms

		List<String> terms = Lists.newArrayList("a", "b", "c", "d", "e", "f",
				"g", "h", "i", "j", "k", "l", "m", "n", "o", "p", "q", "r",
				"s", "t", "u", "v", "w", "x", "y", "z", ".", ",", "!", "#",
				"@", ":");
		hosebirdEndpoint.trackTerms(terms);

		List<String> languages = Lists.newArrayList("fr");
		hosebirdEndpoint.languages(languages);

        // These secrets should be read from a config file
		String ConsumerKey = config.getString("ConsumerKey");
		String ConsumerSecret = config.getString("ConsumerSecret");
		String AccessToken = config.getString("AccessToken");
		String AccessSecret = config.getString("AccessSecret");

		if (ConsumerKey.isEmpty() || ConsumerSecret.isEmpty()
				|| AccessToken.isEmpty() || AccessSecret.isEmpty()) {
			logger.error("Need authentification information");
			return;
		}

		Authentication hosebirdAuth = new OAuth1(ConsumerKey, ConsumerSecret,
				AccessToken, AccessSecret);

		ClientBuilder builder = new ClientBuilder()
				.name("Hosebird-Client-01")
				// optional: mainly for the logs
				.hosts(hosebirdHosts).authentication(hosebirdAuth)
				.endpoint(hosebirdEndpoint)
				.processor(new StringDelimitedProcessor(msgQueue));

		Client hosebirdClient = builder.build();
		// Attempts to establish a connection.
        hosebirdClient.connect();

        CircularFifoQueue<String> idQueue = new CircularFifoQueue<String>(config.getInt("idQueueSize", 100));

		while (!hosebirdClient.isDone()) {
			String msg = msgQueue.take();
			JSONObject json = new JSONObject(msg);
            if (json.has("id_str") && json.has("text")) {
                String id_str = json.getString("id_str");

                if (! idQueue.contains(id_str)) {
                    idQueue.add(id_str);
                    logger.trace(id_str + "\t"
                                 + json.getString("text").replaceAll("\\r|\\n|\\t", " "));
                }
			}
		}
 }
}
