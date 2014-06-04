/*
 * Copyright 2013 Basho Technologies Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.basho.riak.client.operations.kv;

import com.basho.riak.client.RiakCommand;
import com.basho.riak.client.core.RiakCluster;
import com.basho.riak.client.core.RiakFuture;
import com.basho.riak.client.core.operations.ListKeysOperation;
import com.basho.riak.client.operations.CoreFutureAdapter;
import com.basho.riak.client.query.Location;
import com.basho.riak.client.util.BinaryValue;

import java.util.Iterator;
import java.util.List;

 /*
 * @author Dave Rusek <drusek at basho dot com>
 * @since 2.0
 */
public final class ListKeys extends RiakCommand<ListKeys.Response, Location>
{

	private final Location location;
	private final int timeout;

	ListKeys(Builder builder)
	{
		this.location = builder.location;
		this.timeout = builder.timeout;
	}

	@Override 
    protected final RiakFuture<ListKeys.Response, Location> executeAsync(RiakCluster cluster)
    {
        RiakFuture<ListKeysOperation.Response, Location> coreFuture = 
            cluster.execute(buildCoreOperation());
        
        CoreFutureAdapter<ListKeys.Response, Location, ListKeysOperation.Response, Location> future =
            new CoreFutureAdapter<ListKeys.Response, Location, ListKeysOperation.Response, Location>(coreFuture)
            {
                @Override
                protected Response convertResponse(ListKeysOperation.Response coreResponse)
                {
                    return new Response(location.getBucketType(), location.getBucketName(), coreResponse.getKeys());
                }

                @Override
                protected Location convertQueryInfo(Location coreQueryInfo)
                {
                    return coreQueryInfo;
                }
            };
        coreFuture.addListener(future);
        return future;
    }
    
    private ListKeysOperation buildCoreOperation()
    {
        ListKeysOperation.Builder builder = new ListKeysOperation.Builder(location);

		if (timeout > 0)
		{
			builder.withTimeout(timeout);
		}

		return builder.build(); 
    }
    
	public static class Response implements Iterable<Location>
	{

		private final BinaryValue bucket;
        private final BinaryValue type;
		private final List<BinaryValue> keys;

		public Response(BinaryValue type, BinaryValue bucket, List<BinaryValue> keys)
		{
			this.bucket = bucket;
			this.keys = keys;
            this.type = type;
		}

		@Override
		public Iterator<Location> iterator()
		{
			return new Itr(type, bucket, keys.iterator());
		}
	}

	private static class Itr implements Iterator<Location>
	{
		private final Iterator<BinaryValue> iterator;
		private final BinaryValue bucket;
        private final BinaryValue type;

		private Itr(BinaryValue type, BinaryValue bucket, Iterator<BinaryValue> iterator)
		{
			this.iterator = iterator;
			this.bucket = bucket;
            this.type = type;
		}

		@Override
		public boolean hasNext()
		{
			return iterator.hasNext();
		}

		@Override
		public Location next()
		{
			BinaryValue key = iterator.next();
			return new Location(bucket).setBucketType(type).setKey(key);
		}

		@Override
		public void remove()
		{
			iterator.remove();
		}
	}

	public static class Builder
	{
		private final Location location;
		private int timeout;

		public Builder(Location location)
		{
			this.location = location;
		}

		public Builder withTimeout(int timeout)
		{
			this.timeout = timeout;
			return this;
		}

		public ListKeys build()
		{
			return new ListKeys(this);
		}
	}

}
