import 'package:flutter/material.dart';

import 'chat_home_page.dart';

void main() {
  runApp(const MyApp());
}

class MyApp extends StatelessWidget {
  const MyApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'ZzionChat',
      debugShowCheckedModeBanner: false,
      theme: ThemeData(
        useMaterial3: true,
        scaffoldBackgroundColor: const Color(0xFFF5F5F7),
        colorScheme: ColorScheme.fromSeed(
          seedColor: const Color(0xFF1C1C1E),
          brightness: Brightness.light,
        ),
      ),
      home: const ChatHomePage(),
    );
  }
}
