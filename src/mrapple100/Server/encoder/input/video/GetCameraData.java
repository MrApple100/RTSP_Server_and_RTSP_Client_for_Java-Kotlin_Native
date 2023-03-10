/*
 * Copyright (C) 2021 pedroSG94.
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

package mrapple100.Server.encoder.input.video;


import mrapple100.Server.encoder.Frame;
import mrapple100.Server.rtspserver.RtspServerCamera1;

/**
 * Created by pedro on 20/01/17.
 */

public interface GetCameraData {

  //void inputYUVData(Frame frame);

    void inputYUVData(RtspServerCamera1 rtspServerCamera1, Frame frame);
}
