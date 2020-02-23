package ftp;

import lombok.extern.log4j.Log4j2;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.IntegrationFlows;
import org.springframework.integration.dsl.MessageChannels;
import org.springframework.integration.event.inbound.ApplicationEventListeningMessageProducer;
import org.springframework.integration.ftp.server.ApacheMinaFtpEvent;
import org.springframework.integration.ftp.server.ApacheMinaFtplet;
import org.springframework.integration.handler.GenericHandler;
import org.springframework.messaging.MessageChannel;

@Log4j2
@Configuration
class IntegrationConfiguration {

	@Bean
	ApacheMinaFtplet apacheMinaFtplet() {
		return new ApacheMinaFtplet();
	}

	@Bean
	MessageChannel eventsChannel() {
		return MessageChannels.direct().get();
	}

	@Bean
	IntegrationFlow integrationFlow() {
		return IntegrationFlows.from(this.eventsChannel())
			.handle((GenericHandler<ApacheMinaFtpEvent>) (apacheMinaFtpEvent, messageHeaders) -> {
				log.info("new event: " + apacheMinaFtpEvent.getClass().getName() + ':' + apacheMinaFtpEvent.getSession());
				return null;
			})
			.get();
	}

	@Bean
	ApplicationEventListeningMessageProducer applicationEventListeningMessageProducer() {
		var producer = new ApplicationEventListeningMessageProducer();
		producer.setEventTypes(ApacheMinaFtpEvent.class);
		producer.setOutputChannel(eventsChannel());
		return producer;
	}
}
