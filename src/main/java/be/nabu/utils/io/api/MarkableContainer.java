/*
* Copyright (C) 2014 Alexander Verbruggen
*
* This program is free software: you can redistribute it and/or modify
* it under the terms of the GNU Lesser General Public License as published by
* the Free Software Foundation, either version 3 of the License, or
* (at your option) any later version.
*
* This program is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
* GNU Lesser General Public License for more details.
*
* You should have received a copy of the GNU Lesser General Public License
* along with this program. If not, see <https://www.gnu.org/licenses/>.
*/

package be.nabu.utils.io.api;

public interface MarkableContainer<T extends Buffer<T>> extends ResettableContainer<T> {
	/**
	 * You can mark the container, any reset after this point will reset it to the given mark
	 * There is no read limit though specific implementations might provide this
	 */
	public void mark();
	public void unmark();
	/**
	 * At worst this is an unmark() + mark(), at best it can be a more performant option
	 */
	public void remark();
}
