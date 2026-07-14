package com.hmxpanel.aiide.logsender;

interface ILogSender {

  void ping();

  void startReader(int port);

  int getPid();

  String getPackageName();

  String getId();

  void onDisconnect();
}
