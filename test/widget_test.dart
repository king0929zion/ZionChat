import 'package:flutter_test/flutter_test.dart';

import 'package:zzionchat/main.dart';

void main() {
  testWidgets('chat shell renders initial conversation', (tester) async {
    await tester.pumpWidget(const MyApp());

    expect(find.text('ChatGPT'), findsOneWidget);
    expect(find.text('哈喽'), findsOneWidget);
    expect(find.text('Message'), findsOneWidget);
  });
}
