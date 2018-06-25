package screenshooter;

import java.awt.AWTException;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;
import java.util.Date;

import javax.imageio.ImageIO;

import io.micronaut.http.HttpHeaders;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;

@Controller("/shooter")
public class Shooter {

	private BufferedImage capture;
	private Date captureTime;

	@Get(value="/",produces={ "image/jpeg"})
	public HttpResponse<?> getScreenshoot(HttpRequest<?> req) throws IOException, AWTException {

		Calendar lastShootMin = Calendar.getInstance();
		lastShootMin.add(Calendar.SECOND, -5);

		Date lastShootMinDate = lastShootMin.getTime();
		if(captureTime == null || captureTime.compareTo(lastShootMinDate) < 0) {
			Rectangle screenRect = new Rectangle(Toolkit.getDefaultToolkit().getScreenSize());
			BufferedImage currentCapture = new Robot().createScreenCapture(screenRect);
			capture = currentCapture;
			captureTime = new Date(new Date().getTime() / 1000 * 1000);
		}

		String ifModSince = req.getHeaders().get(HttpHeaders.IF_MODIFIED_SINCE);
		if(ifModSince != null && captureTime != null) {
			ZonedDateTime zdt = ZonedDateTime.parse(ifModSince, DateTimeFormatter.RFC_1123_DATE_TIME);
			if(zdt.toInstant().compareTo(captureTime.toInstant()) >= 0) {
				return HttpResponse.notModified();
			}
		}

		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		ImageIO.write(capture, "jpg", baos);
		return HttpResponse
				.ok(baos.toByteArray())
				.header(HttpHeaders.LAST_MODIFIED, DateTimeFormatter.RFC_1123_DATE_TIME.format(captureTime.toInstant().atZone(ZoneOffset.UTC)));
	}
}
