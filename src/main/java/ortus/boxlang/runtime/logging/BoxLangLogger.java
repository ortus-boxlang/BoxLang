/**
 * [BoxLang]
 *
 * Copyright [2023] [Ortus Solutions, Corp]
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ortus.boxlang.runtime.logging;

import java.io.Serializable;
import java.util.Iterator;

import org.slf4j.Marker;
import org.slf4j.event.LoggingEvent;
import org.slf4j.spi.LocationAwareLogger;
import org.slf4j.spi.LoggingEventAware;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
import ch.qos.logback.core.spi.AppenderAttachable;

public class BoxLangLogger implements LocationAwareLogger, LoggingEventAware, AppenderAttachable<ILoggingEvent>, Serializable {

	private Logger logger;

	public BoxLangLogger( Logger logger ) {
		this.logger = logger;
	}

	public void setLevel( int level ) {
		Level newLevel = Level.toLevel( level );
		this.logger.setLevel( newLevel );
	}

	public void setLevel( ch.qos.logback.classic.Level level ) {
		this.logger.setLevel( level );
	}

	public void setLevel( org.slf4j.event.Level level ) {
		this.logger.setLevel( Level.toLevel( level.toInt() ) );
	}

	@Override
	public void addAppender( Appender<ILoggingEvent> newAppender ) {
		this.logger.addAppender( newAppender );
	}

	@Override
	public Iterator<Appender<ILoggingEvent>> iteratorForAppenders() {
		return this.logger.iteratorForAppenders();
	}

	@Override
	public Appender<ILoggingEvent> getAppender( String name ) {
		return this.logger.getAppender( name );
	}

	@Override
	public boolean isAttached( Appender<ILoggingEvent> appender ) {
		return this.logger.isAttached( appender );
	}

	@Override
	public void detachAndStopAllAppenders() {
		this.logger.detachAndStopAllAppenders();
	}

	@Override
	public boolean detachAppender( Appender<ILoggingEvent> appender ) {
		return this.logger.detachAppender( appender );
	}

	@Override
	public boolean detachAppender( String name ) {
		return this.logger.detachAppender( name );
	}

	@Override
	public void log( LoggingEvent event ) {
		this.logger.log( event );
	}

	@Override
	public void log( Marker marker, String fqcn, int level, String message, Object[] argArray, Throwable t ) {
		this.logger.log( marker, fqcn, level, message, argArray, t );
	}

	@Override
	public String getName() {
		return this.logger.getName();
	}

	@Override
	public boolean isTraceEnabled() {
		return this.logger.isTraceEnabled();
	}

	@Override
	public void trace( String msg ) {
		this.logger.trace( msg );
	}

	@Override
	public void trace( String format, Object arg ) {
		this.logger.trace( format, arg );
	}

	@Override
	public void trace( String format, Object arg1, Object arg2 ) {
		this.logger.trace( format, arg1, arg2 );
	}

	@Override
	public void trace( String format, Object... arguments ) {
		this.logger.trace( format, arguments );
	}

	@Override
	public void trace( String msg, Throwable t ) {
		this.logger.trace( msg, t );
	}

	@Override
	public boolean isTraceEnabled( Marker marker ) {
		return this.logger.isTraceEnabled( marker );
	}

	@Override
	public void trace( Marker marker, String msg ) {
		this.logger.trace( marker, msg );
	}

	@Override
	public void trace( Marker marker, String format, Object arg ) {
		this.logger.trace( marker, format, arg );
	}

	@Override
	public void trace( Marker marker, String format, Object arg1, Object arg2 ) {
		this.logger.trace( marker, format, arg1, arg2 );
	}

	@Override
	public void trace( Marker marker, String format, Object... argArray ) {
		this.logger.trace( marker, format, argArray );
	}

	@Override
	public void trace( Marker marker, String msg, Throwable t ) {
		this.logger.trace( marker, msg, t );
	}

	@Override
	public boolean isDebugEnabled() {
		return this.logger.isDebugEnabled();
	}

	@Override
	public void debug( String msg ) {
		this.logger.debug( msg );
	}

	@Override
	public void debug( String format, Object arg ) {
		this.logger.debug( format, arg );
	}

	@Override
	public void debug( String format, Object arg1, Object arg2 ) {
		this.logger.debug( format, arg1, arg2 );
	}

	@Override
	public void debug( String format, Object... arguments ) {
		this.logger.debug( format, arguments );
	}

	@Override
	public void debug( String msg, Throwable t ) {
		this.logger.debug( msg, t );
	}

	@Override
	public boolean isDebugEnabled( Marker marker ) {
		return this.logger.isDebugEnabled( marker );
	}

	@Override
	public void debug( Marker marker, String msg ) {
		this.logger.debug( marker, msg );
	}

	@Override
	public void debug( Marker marker, String format, Object arg ) {
		this.logger.debug( marker, format, arg );
	}

	@Override
	public void debug( Marker marker, String format, Object arg1, Object arg2 ) {
		this.logger.debug( marker, format, arg1, arg2 );
	}

	@Override
	public void debug( Marker marker, String format, Object... arguments ) {
		this.logger.debug( marker, format, arguments );
	}

	@Override
	public void debug( Marker marker, String msg, Throwable t ) {
		this.logger.debug( marker, msg, t );
	}

	@Override
	public boolean isInfoEnabled() {
		return this.logger.isInfoEnabled();
	}

	@Override
	public void info( String msg ) {
		this.logger.info( msg );
	}

	@Override
	public void info( String format, Object arg ) {
		this.logger.info( format, arg );
	}

	@Override
	public void info( String format, Object arg1, Object arg2 ) {
		this.logger.info( format, arg1, arg2 );
	}

	@Override
	public void info( String format, Object... arguments ) {
		this.logger.info( format, arguments );
	}

	@Override
	public void info( String msg, Throwable t ) {
		this.logger.info( msg, t );
	}

	@Override
	public boolean isInfoEnabled( Marker marker ) {
		return this.logger.isInfoEnabled( marker );
	}

	@Override
	public void info( Marker marker, String msg ) {
		this.logger.info( marker, msg );
	}

	@Override
	public void info( Marker marker, String format, Object arg ) {
		this.logger.info( marker, format, arg );
	}

	@Override
	public void info( Marker marker, String format, Object arg1, Object arg2 ) {
		this.logger.info( marker, format, arg1, arg2 );
	}

	@Override
	public void info( Marker marker, String format, Object... arguments ) {
		this.logger.info( marker, format, arguments );
	}

	@Override
	public void info( Marker marker, String msg, Throwable t ) {
		this.logger.info( marker, msg, t );
	}

	@Override
	public boolean isWarnEnabled() {
		return this.logger.isWarnEnabled();
	}

	@Override
	public void warn( String msg ) {
		this.logger.warn( msg );
	}

	@Override
	public void warn( String format, Object arg ) {
		this.logger.warn( format, arg );
	}

	@Override
	public void warn( String format, Object... arguments ) {
		this.logger.warn( format, arguments );
	}

	@Override
	public void warn( String format, Object arg1, Object arg2 ) {
		this.logger.warn( format, arg1, arg2 );
	}

	@Override
	public void warn( String msg, Throwable t ) {
		this.logger.warn( msg, t );
	}

	@Override
	public boolean isWarnEnabled( Marker marker ) {
		return this.logger.isWarnEnabled( marker );
	}

	@Override
	public void warn( Marker marker, String msg ) {
		this.logger.warn( marker, msg );
	}

	@Override
	public void warn( Marker marker, String format, Object arg ) {
		this.logger.warn( marker, format, arg );
	}

	@Override
	public void warn( Marker marker, String format, Object arg1, Object arg2 ) {
		this.logger.warn( marker, format, arg1, arg2 );
	}

	@Override
	public void warn( Marker marker, String format, Object... arguments ) {
		this.logger.warn( marker, format, arguments );
	}

	@Override
	public void warn( Marker marker, String msg, Throwable t ) {
		this.logger.warn( marker, msg, t );
	}

	@Override
	public boolean isErrorEnabled() {
		return this.logger.isErrorEnabled();
	}

	@Override
	public void error( String msg ) {
		this.logger.error( msg );
	}

	@Override
	public void error( String format, Object arg ) {
		this.logger.error( format, arg );
	}

	@Override
	public void error( String format, Object arg1, Object arg2 ) {
		this.logger.error( format, arg1, arg2 );
	}

	@Override
	public void error( String format, Object... arguments ) {
		this.logger.error( format, arguments );
	}

	@Override
	public void error( String msg, Throwable t ) {
		this.logger.error( msg, t );
	}

	@Override
	public boolean isErrorEnabled( Marker marker ) {
		return this.logger.isErrorEnabled( marker );
	}

	@Override
	public void error( Marker marker, String msg ) {
		this.logger.error( marker, msg );
	}

	@Override
	public void error( Marker marker, String format, Object arg ) {
		this.logger.error( marker, format, arg );
	}

	@Override
	public void error( Marker marker, String format, Object arg1, Object arg2 ) {
		this.logger.error( marker, format, arg1, arg2 );
	}

	@Override
	public void error( Marker marker, String format, Object... arguments ) {
		this.logger.error( marker, format, arguments );
	}

	@Override
	public void error( Marker marker, String msg, Throwable t ) {
		this.logger.error( marker, msg, t );
	}

}
