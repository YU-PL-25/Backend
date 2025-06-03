package PL_25.shuttleplay;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.ServletComponentScan;

@SpringBootApplication
@ServletComponentScan
public class ShuttleplayApplication {

	public static void main(String[] args) {
		SpringApplication.run(ShuttleplayApplication.class, args);
	}

}
