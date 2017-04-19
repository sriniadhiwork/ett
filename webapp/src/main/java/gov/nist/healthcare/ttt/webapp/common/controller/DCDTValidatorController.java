package gov.nist.healthcare.ttt.webapp.common.controller;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.apache.log4j.Logger;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import gov.nist.healthcare.ttt.webapp.common.model.exceptionJSON.TTTCustomException;

@Controller
@RequestMapping("/api/dcdt")
public class DCDTValidatorController {

	public static final String DCDT_HOSTING_URL = "http://dcdt31prod.sitenv.org:8080/dcdt/hosting/process";

	@Value("${ett.dcdt.2014.url}")
	String dcdt2014Url;

	@Value("${ett.dcdt.2015.url}")
	String dcdt2015Url;

	private static Logger logger = Logger.getLogger(DCDTValidatorController.class.getName());

	@RequestMapping(method = RequestMethod.POST, produces = "application/json")
	public @ResponseBody String validateDCDT(@RequestBody HashMap<String, String> filePath) throws Exception {


				logger.info("Validating DCDT "+dcdt2015Url);
				String message;
				String result = "";
				String line;
				StringBuilder output;

				CloseableHttpClient client = HttpClients.createDefault();
				HttpPost post = new HttpPost(DCDT_HOSTING_URL);
				//
				JSONObject jo = new JSONObject();
				jo.put("@type", "hostingTestcaseSubmission");
				jo.put("directAddr", "provider1@direct.sitenv.org");
				jo.put("testcase", "H2_DNS_AB_Normal");

				message= jo.toString();

				logger.info("message "+message);

				try {
					post.setEntity(new StringEntity(message));
					post.setHeader("Content-type", "application/json");
					post.setHeader("accept", "application/json");
post.setProtocolVersion(HttpVersion.HTTP_1_0);


					HttpResponse response = client.execute(post);
					logger.info("Status Code DCDT "+ response.getStatusLine().getStatusCode());
					logger.info("Validating response.getEntity() "+ response.getEntity());
					if (response.getStatusLine().getStatusCode() != 200)
					{
						throw new TTTCustomException("0x0077","Failed : HTTP error code : " + response.getStatusLine().getStatusCode());
					}

					/*
					 * 	Read response from DCDT.
					 */
					/*
					BufferedReader br = new BufferedReader(new InputStreamReader((response.getEntity().getContent())));
					output = new StringBuilder();
					while ((line = br.readLine()) != null)
					{
						output.append(line);
					}
					client.getConnectionManager().shutdown();
					result = output.toString();
*/
					// CONVERT RESPONSE TO STRING
					HttpEntity entity = response.getEntity();
					if (entity != null) {
						result =  EntityUtils.toString(entity);
					}
					//result = EntityUtils.toString(response.getEntity());


	/*				RestTemplate restTemplate = new RestTemplate();
					//HttpMessageConverter formHttpMessageConverter = new FormHttpMessageConverter();
					//HttpMessageConverter stringHttpMessageConverternew = new StringHttpMessageConverter();
					//restTemplate.setMessageConverters(new HttpMessageConverter[]{formHttpMessageConverter, stringHttpMessageConverternew});
					MultiValueMap<String, String> map = new LinkedMultiValueMap<String, String>();
					map.add("@type", "hostingTestcaseSubmission");
					map.add("directAddr", "provider1@direct.sitenv.org");
					map.add("testcase", "H2_DNS_AB_Normal");

					result = restTemplate.postForObject(DCDT_HOSTING_URL, map, String.class);
					
*/
					//ResponseEntity<String> responseObj = restTemplate.getForEntity(DCDT_HOSTING_URL,String.class);
					System.out.println(result);
					//System.out.println(responseObj);
				} catch (UnsupportedEncodingException e1) {
					logger.error(e1.getMessage());
					throw new TTTCustomException("0x0076", "An error occured while creating json from file UnsupportedEncodingException");
				}

				System.out.println("test json string1111"+jo.toString());
				logger.info("test json string11112222"+jo.toString());
				return jo.toString();
	}
}