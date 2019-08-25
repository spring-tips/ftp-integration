package ftp;

import lombok.extern.log4j.Log4j2;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.IntegrationFlows;
import org.springframework.integration.dsl.MessageChannels;
import org.springframework.integration.event.inbound.ApplicationEventListeningMessageProducer;
import org.springframework.integration.ftp.server.ApacheMinaFtpEvent;
import org.springframework.integration.handler.GenericHandler;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHeaders;

@Log4j2
@Configuration
class IntegrationConfiguration {

	@Bean
	MessageChannel eventsChannel() {
		return MessageChannels.direct().get();
	}

	@Bean
	ApplicationEventListeningMessageProducer eventsAdapter() {
		ApplicationEventListeningMessageProducer producer = new ApplicationEventListeningMessageProducer();
		producer.setEventTypes(ApacheMinaFtpEvent.class);
		producer.setOutputChannel(eventsChannel());
		return producer;
	}

	@Bean
	IntegrationFlow processEvents() {
		return IntegrationFlows
			.from(this.eventsChannel())
			.handle((GenericHandler<ApacheMinaFtpEvent>) (ftpEvent, messageHeaders) -> {
				log.info("new " + ftpEvent.getClass().getName() + ":" + ftpEvent.getSession());
				return null;
			})
			.get();
	}

}
