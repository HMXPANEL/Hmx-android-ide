package com.hmxpanel.aiide.logsender;

import com.hmxpanel.aiide.logsender.ILogSender;

oneway interface ILogReceiver {

  void ping();

  void connect(ILogSender sender);

  void disconnect(String packageName, String senderId);
}
