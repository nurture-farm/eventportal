/*
 *  Copyright 2023 NURTURE AGTECH PVT LTD
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package farm.nurture.eventportal.repository;

import com.google.inject.Singleton;
import farm.nurture.infra.util.StringUtils;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

@Singleton
public class QueryBuilder {

    @Data
    @AllArgsConstructor
    public static final class Query {
        private String query;

        private List<Object> params;
    }


    public Query buildFilterEventsQuery(String namespace, String name, boolean paginationFilter, int pageNumber, int limit) {

        if(pageNumber==0) {
            pageNumber = 1;
        }
        if(limit==0) {
            limit = 20;
        }
        StringBuilder query = new StringBuilder(EventRepository.partialFilterEventSql);
        List<Object> params = new ArrayList<>();

        if(StringUtils.isNonEmpty(namespace)) {
            query.append("and ");
            query.append("namespace = ? ");
            params.add(namespace);
        }
        if(StringUtils.isNonEmpty(name)) {
            query.append("and ");
            query.append("name = ? ");
            params.add(name);
        }
        if(paginationFilter) {
            Query pageinationFilter = buildPaginationFilter(pageNumber, limit);
            query.append(pageinationFilter.query);
            pageinationFilter.getParams().forEach(param -> params.add(param));
        }
        return new Query(query.toString(), params);
    }

    public Query buildFilterEventPropertiesQuery(Long eventId, boolean paginationFilter, int pageNumber, int limit) {

        if(pageNumber==0) {
            pageNumber = 1;
        }
        if(limit==0) {
            limit = 20;
        }
        StringBuilder query = new StringBuilder(EventPropertyRepository.selectEventPropertyByEventId);
        List<Object> params = new ArrayList<>();
        params.add(eventId);
        if(paginationFilter) {
            Query pageinationFilter = buildPaginationFilter(pageNumber, limit);
            query.append(pageinationFilter.query);
            pageinationFilter.getParams().forEach(param -> params.add(param));
        }
        return new Query(query.toString(), params);
    }

    public Query buildPaginationFilter( int pageNumber, int limit) {

        StringBuilder query = new StringBuilder();
        List<Object> params = new ArrayList<>();

        query.append("LIMIT ? OFFSET ?");
        params.add(limit);
        params.add(limit*(pageNumber-1));
        return new Query(query.toString(), params);
    }
}
