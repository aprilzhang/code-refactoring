package crawler;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.mail.Authenticator;
import javax.mail.Message.RecipientType;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

import bean.Stock;

public class XueqiuCrawler {
	private  static final String CONFIG_FILE = "./config.properties";
	private static final String KEY_USERNAME = "user";
	private static final  String KEY_PASSWORD = "password";
	private static final  String KEY_FROM_EMAIL_USERNAME = "from_email_user";
	private static final  String KEY_FROM_EMAIL_PASSWORD = "from_email_pw";
	private static final  String KEY_TO_EMAIL = "to_email";
	private static final  String LOGIN_URL = "http://xueqiu.com/user/login";
	private static final String STOCK_URL = "http://xueqiu.com/cubes/rebalancing/history.json?cube_symbol=ZH010389&count=20&page=1";

	private static final DateFormat DATE_FORMAT = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");

	public static void main(final String[] args) {

		final CloseableHttpClient httpclient = HttpClientBuilder.create().build();

		//1,读取相关参数
		//data.ini文件内容：雪球账号#雪球密码#发件邮箱#邮箱账号#收件人
		//暂时支持qq邮箱为发件邮箱
		final Properties configuration = loadData();

		//2，模拟用户登陆
		login(httpclient, configuration);

		//3，得到原始数据
		final HttpEntity entity = getHtmlEntity(httpclient, STOCK_URL);
		String body = null;
		try {
			body = EntityUtils.toString(entity);
		} catch (final Exception e) {
			e.printStackTrace();
		}
		System.out.println("body:" + body);

		//4，抽取需要数据
		final List<Object> results = parseJson(body);

		final List<Stock> stocks = new ArrayList<>();
		for(final Object obj :results){
			final Map stockMap = JSON.parseObject(obj.toString(), Map.class);

			final Stock stock = new Stock();
			stock.setName((String) stockMap.get("stock_name"));
			stock.setSymbol((String) stockMap.get("stock_symbol"));
			stock.setPrice((BigDecimal) stockMap.get("price"));
			stock.setPrev_weight((BigDecimal) stockMap.get("prev_weight"));
			stock.setTarget_weight((BigDecimal) stockMap.get("target_weight"));
			stock.setUpdateTime(DATE_FORMAT.format(new Timestamp((long) stockMap.get("updated_at"))));

			stocks.add(stock);
		}

		//5，发送邮件
		try {
			sendEmail(configuration, stocks);
		} catch (final MessagingException e) {
			e.printStackTrace();
		}
	}

	/**
	 * load user config into properties
	 * @return
	 */
	private static Properties loadData() {
		final Properties prop = new Properties();
		try (InputStream input = new FileInputStream(CONFIG_FILE))
		{
			prop.load(input);
		}
		catch (final IOException ex)
		{
			ex.printStackTrace();
		}
		return prop;
	}


	/**
	 * 发送邮件
	 *
	 * @param config
	 * @param stocks
	 * @throws MessagingException
	 */
	private static void sendEmail(final Properties config, final List<Stock> stocks) throws MessagingException {
		final Properties properties = new Properties();

		// 表示SMTP发送邮件，需要进行身份验证
		properties.put("mail.smtp.auth", "true");
		properties.put("mail.smtp.host", "smtp.qq.com");

		// 发件人的账号
		properties.put("mail.user", config.get(KEY_FROM_EMAIL_USERNAME));
		// 访问SMTP服务时需要提供的密码
		properties.put("mail.password", config.getProperty(KEY_FROM_EMAIL_PASSWORD));

		// 构建授权信息，用于进行SMTP进行身份验证
		final Authenticator authenticator = new Authenticator(){
			@Override
			protected PasswordAuthentication getPasswordAuthentication() {
				final String userName = properties.getProperty("mail.user");
				final String password = properties.getProperty("mail.password");
				return new PasswordAuthentication(userName, password);
			}
		};

		// 使用环境属性和授权信息，创建邮件会话
		final Session mailSession = Session.getInstance(properties, authenticator);

		// 创建邮件消息
		final MimeMessage message = new MimeMessage(mailSession);
		// 设置发件人
		final InternetAddress form = new InternetAddress(
				properties.getProperty("mail.user"));
		message.setFrom(form);

		// 设置收件人
		final InternetAddress to = new InternetAddress(config.getProperty(KEY_TO_EMAIL));
		message.setRecipient(RecipientType.TO, to);

		// 设置邮件标题
		message.setSubject("java 测试");
		// 设置邮件的内容体
		message.setContent(stocks.toString(),"text/html;charset=UTF-8");

		// 发送邮件
		Transport.send(message);
		System.out.println("发送邮件完毕");
	}



	private static List<Object> parseJson(final String body) {
		final JSONObject obj = JSON.parseObject(body);
		final Object list = obj.get("list");
		final JSONArray jsonList = JSON.parseArray(list.toString());
		final JSONObject histories = (JSONObject) jsonList.get(0);
		final Object jsonSockList = histories.get("rebalancing_histories");
		final List<Object> Socklist = JSON.parseArray(jsonSockList.toString());

		return Socklist;
	}

	/**
	 *
	 * @param httpclient
	 * @param stockUrl
	 * @return
	 */
	private static HttpEntity getHtmlEntity(final CloseableHttpClient httpclient, final String stockUrl) {
		final HttpGet httpGet = new HttpGet(stockUrl);
		HttpResponse resp = null;
		try {
			resp = httpclient.execute(httpGet);
			if(resp.getStatusLine().getStatusCode() == 400){
				for(final Header header : resp.getAllHeaders()){
					System.out.println(header.getName() + " : " + header.getValue());
				}
			}else{
				System.out.println("statusCode " + resp.getStatusLine().getStatusCode());
			}

		} catch (final Exception e) {
			e.printStackTrace();
		}
		return resp.getEntity();
	}

	/**
	 *
	 * @param httpClient
	 * @param configuration
	 */
	private static void login(final CloseableHttpClient httpClient, final Properties configuration) {

		try {
			final HttpPost post = new HttpPost(LOGIN_URL);
			post.addHeader("User-Agent","Mozilla/5.0 (Windows NT 6.3; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/36.0.1985.125 Safari/537.36");

			final List<NameValuePair> nvps = new ArrayList<>();
			nvps.add(new BasicNameValuePair("username", configuration.getProperty(KEY_USERNAME)));
			nvps.add(new BasicNameValuePair("password", configuration.getProperty(KEY_PASSWORD)));
			post.setEntity(new UrlEncodedFormEntity(nvps,"utf-8"));

			final HttpResponse loginResponse = httpClient.execute(post);
			System.out.println("statusCode :" + loginResponse.getStatusLine().getStatusCode());
			if (loginResponse.getStatusLine().getStatusCode() == 302) {
				final String locationUrl=loginResponse.getLastHeader("Location").getValue();
				getHtmlEntity(httpClient, locationUrl);// 跳转到重定向的url
			}
			post.releaseConnection();

		} catch (final Exception e) {
			e.printStackTrace();
		}
	}




}
