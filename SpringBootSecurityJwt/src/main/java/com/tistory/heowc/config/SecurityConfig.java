package com.tistory.heowc.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tistory.heowc.auth.BaseSecurityHandler;
import com.tistory.heowc.auth.ajax.AjaxAuthenticationProvider;
import com.tistory.heowc.auth.ajax.filter.AjaxAuthenticationFilter;
import com.tistory.heowc.auth.jwt.JwtAuthenticationProvider;
import com.tistory.heowc.auth.jwt.filter.JwtAuthenticationFilter;
import com.tistory.heowc.auth.jwt.matcher.SkipPathRequestMatcher;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

import java.util.Arrays;

@Configuration
@EnableWebSecurity
@EnableGlobalMethodSecurity(prePostEnabled = true)
@RequiredArgsConstructor
public class SecurityConfig extends WebSecurityConfigurerAdapter {

	private final JwtAuthenticationProvider jwtProvider;
	private final AjaxAuthenticationProvider ajaxProvider;

	private final BaseSecurityHandler securityHandler;

	private final ObjectMapper objectMapper;
	
	private static final String LOGIN_END_POINT = "/login";
	private static final String TOKEN_END_POINT = "/token";
	private static final String ROOT_END_POINT  = "/**";
	
	@Override
	public void configure(WebSecurity web) throws Exception {
		web.ignoring().antMatchers("/resources/**");
	}
	
	@Override
	protected void configure(AuthenticationManagerBuilder auth) throws Exception {
		auth.authenticationProvider(ajaxProvider)
			.authenticationProvider(jwtProvider);
	}

	@Override
	protected void configure(HttpSecurity http) throws Exception {
		http
			.addFilterBefore(ajaxAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class)
			.addFilterBefore(jwtAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class)
			.csrf().disable()
			.sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS)
			.and()
				.authorizeRequests()
				.antMatchers(TOKEN_END_POINT).permitAll()
				.antMatchers(LOGIN_END_POINT).permitAll()
				.antMatchers(ROOT_END_POINT).authenticated()
			.and()
				.formLogin();
//				.loginProcessingUrl(TOKEN_END_POINT);
	}
	
	private AntPathRequestMatcher antPathRequestMatcher() {
		return new AntPathRequestMatcher(TOKEN_END_POINT, HttpMethod.POST.name());
	}
	
	private AjaxAuthenticationFilter ajaxAuthenticationFilter() throws Exception {
		AjaxAuthenticationFilter filter = new AjaxAuthenticationFilter(antPathRequestMatcher(), objectMapper);
		filter.setAuthenticationManager(authenticationManager());
		filter.setAuthenticationSuccessHandler(securityHandler);
		filter.setAuthenticationFailureHandler(securityHandler);
		return filter;
	}
	
	private SkipPathRequestMatcher skipPathRequestMatcher() {
		return new SkipPathRequestMatcher(Arrays.asList(LOGIN_END_POINT, TOKEN_END_POINT));
	}
	
	private JwtAuthenticationFilter jwtAuthenticationFilter() throws Exception {
		JwtAuthenticationFilter filter = new JwtAuthenticationFilter(skipPathRequestMatcher());
		filter.setAuthenticationManager(authenticationManager());
		filter.setAuthenticationFailureHandler(securityHandler);
		return filter;
	}
}