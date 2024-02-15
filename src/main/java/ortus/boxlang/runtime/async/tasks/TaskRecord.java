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
package ortus.boxlang.runtime.async.tasks;

import java.time.LocalDateTime;
import java.util.concurrent.ScheduledFuture;

/**
 * The task record holds all the information of a living task in the scheduler.
 */
public class TaskRecord {

	/**
	 * Task name
	 */
	public String				name;
	/**
	 * Task group
	 */
	public String				group;
	/**
	 * The task object
	 */
	public ScheduledTask		task;
	/**
	 * The future object for the task
	 */
	public ScheduledFuture<?>	future;
	/**
	 * The scheduled date for the task
	 */
	public LocalDateTime		scheduledAt;
	/**
	 * The registered date for the task
	 */
	public LocalDateTime		registeredAt;
	/**
	 * If the task is disabled
	 */
	public Boolean				disabled		= false;
	/**
	 * If the task errored out when scheduling
	 */
	public Boolean				error			= false;
	/**
	 * The error message if any
	 */
	public String				errorMessage	= "";
	/**
	 * The stacktrace if any
	 */
	public String				stacktrace		= "";
	/**
	 * The inet host
	 */
	public String				inetHost		= "";
	/**
	 * The inet address
	 */
	public String				localIp			= "";

	/**
	 * Construct the record
	 *
	 * @param name  The name of the task
	 * @param group The group of the task
	 * @param task  The task object
	 */
	public TaskRecord( String name, String group, ScheduledTask task ) {
		this.name			= name;
		this.group			= group;
		this.task			= task;
		this.registeredAt	= LocalDateTime.now( task.getTimezone() );
	}
}
