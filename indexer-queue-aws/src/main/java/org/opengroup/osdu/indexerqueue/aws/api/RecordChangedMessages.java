// Copyright © Amazon Web Services
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package org.opengroup.osdu.indexerqueue.aws.api;

import java.util.Map;

// TODO: consolidate this model with core refactor
public class RecordChangedMessages {
    public String messageId;
    public String publishTime;
    public String data;
    public Map<String, String> attributes;

    public String getMessageId(){
        return this.messageId;
    }

    /*public String getData(){
        return this.data;
    }

    public Map<String, String> getAttributes(){
        return this.attributes;
    }

    public void setMessageId(String newMessageId){
        this.messageId = newMessageId;
    }

    public void setData(String newData){
        this.data = newData;
    }

    public void setAttributes(Map<String, String> newAttributes){
        this.attributes = newAttributes;
    }*/
}
