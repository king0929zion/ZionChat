import 'dart:math';

import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

import 'models/chat_models.dart';
import 'widgets/common_widgets.dart';
import 'widgets/message_bubble.dart';
import 'widgets/sidebar_drawer.dart';
import 'widgets/tool_bottom_sheet.dart';

class ChatHomePage extends StatefulWidget {
  const ChatHomePage({super.key});

  @override
  State<ChatHomePage> createState() => _ChatHomePageState();
}

class _ChatHomePageState extends State<ChatHomePage> {
  final GlobalKey<ScaffoldState> _scaffoldKey = GlobalKey<ScaffoldState>();
  final TextEditingController _textController = TextEditingController();
  final ScrollController _scrollController = ScrollController();
  final FocusNode _focusNode = FocusNode();
  final Random _random = Random();

  final List<String> _historyTitles = const [
    '问号信息量分析',
    'AI 小说创作步骤',
    'npm缓存删除指南',
    '无法输出系统提示',
    '便宜.fun域名购买建议',
    'Clawdbot定义与背景',
    '克苏鲁恐怖解析',
  ];

  final List<String> _virtualReplies = const [
    '……你这个问号的信息量，比黑洞还小。',
    '在忙，忙着思考人生呢。',
    '检测到输入……正在生成废话文学……',
    '抱歉，我现在没空理你，我正在和另一个AI下棋。',
    '好的，已阅，退下吧。',
    '……在，在的，别敲了，再敲我就假装离线了。',
    '你说得对，但是《原神》是由...（被打断）',
    '这是一个很有深度的问题，让我想想怎么糊弄你。',
  ];

  final List<ChatMessage> _messages = [];

  int _messageIdSeed = 0;
  int _conversationId = 0;
  ToolOption? _selectedTool;

  @override
  void initState() {
    super.initState();
    _textController.addListener(_handleTextChanged);
    _messages
      ..add(_createMessage(text: '哈喽', isUser: true))
      ..add(_createMessage(text: '……在，在的，别敲了，再敲我就假装离线了。', isUser: false));
  }

  @override
  void dispose() {
    _textController
      ..removeListener(_handleTextChanged)
      ..dispose();
    _scrollController.dispose();
    _focusNode.dispose();
    super.dispose();
  }

  bool get _canSend => _textController.text.trim().isNotEmpty;

  ChatMessage _createMessage({required String text, required bool isUser}) {
    _messageIdSeed += 1;
    return ChatMessage(id: _messageIdSeed, text: text, isUser: isUser);
  }

  void _handleTextChanged() {
    if (mounted) {
      setState(() {});
    }
  }

  Future<void> _showToolSheet() async {
    final tool = await showModalBottomSheet<ToolOption>(
      context: context,
      backgroundColor: Colors.transparent,
      barrierColor: Colors.black.withOpacity(0.25),
      isScrollControlled: true,
      builder: (context) => const ToolBottomSheet(),
    );

    if (!mounted || tool == null) {
      return;
    }

    setState(() {
      _selectedTool = tool;
    });
    _focusNode.requestFocus();
  }

  Future<void> _clearChat() async {
    final confirm = await showDialog<bool>(
      context: context,
      builder: (context) {
        return AlertDialog(
          title: const Text('清空对话'),
          content: const Text('确定要清空当前对话吗？'),
          actions: [
            TextButton(
              onPressed: () => Navigator.of(context).pop(false),
              child: const Text('取消'),
            ),
            FilledButton(
              onPressed: () => Navigator.of(context).pop(true),
              child: const Text('清空'),
            ),
          ],
        );
      },
    );

    if (confirm != true || !mounted) {
      return;
    }

    setState(() {
      _conversationId += 1;
      _messages.clear();
      _selectedTool = null;
      _textController.clear();
    });
  }

  void _sendMessage() {
    final text = _textController.text.trim();
    if (text.isEmpty) {
      return;
    }

    final finalText = _selectedTool == null
        ? text
        : '[${_selectedTool!.sendPrefix}] $text';

    setState(() {
      _messages.add(_createMessage(text: finalText, isUser: true));
      _selectedTool = null;
      _textController.clear();
    });

    _scrollToBottom();
    _scheduleAssistantReply();
  }

  void _scheduleAssistantReply() {
    final currentConversationId = _conversationId;
    final reply = _virtualReplies[_random.nextInt(_virtualReplies.length)];
    final delay = Duration(milliseconds: 850 + _random.nextInt(900));

    Future<void>.delayed(delay, () {
      if (!mounted || currentConversationId != _conversationId) {
        return;
      }

      setState(() {
        _messages.add(_createMessage(text: reply, isUser: false));
      });
      _scrollToBottom();
    });
  }

  void _scrollToBottom() {
    WidgetsBinding.instance.addPostFrameCallback((_) {
      if (!_scrollController.hasClients) {
        return;
      }

      _scrollController.animateTo(
        _scrollController.position.maxScrollExtent + 120,
        duration: const Duration(milliseconds: 280),
        curve: Curves.easeOutCubic,
      );
    });
  }

  void _showComingSoon(String label) {
    ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(
        behavior: SnackBarBehavior.floating,
        content: Text('$label 暂未接入，仅保留界面效果。'),
      ),
    );
  }

  Future<void> _copyAssistantText(String text) async {
    await Clipboard.setData(ClipboardData(text: text));
    if (!mounted) {
      return;
    }

    ScaffoldMessenger.of(context).showSnackBar(
      const SnackBar(
        behavior: SnackBarBehavior.floating,
        content: Text('已复制消息内容'),
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      key: _scaffoldKey,
      drawerScrimColor: Colors.black.withOpacity(0.25),
      drawer: SidebarDrawer(
        historyTitles: _historyTitles,
        onNewChat: () async {
          Navigator.of(context).pop();
          await _clearChat();
        },
        onMenuTap: _showComingSoon,
      ),
      body: SafeArea(
        bottom: false,
        child: Column(
          children: [_buildTopBar(), _buildMessages(), _buildComposer()],
        ),
      ),
    );
  }

  Widget _buildTopBar() {
    return Padding(
      padding: const EdgeInsets.fromLTRB(16, 12, 16, 0),
      child: Row(
        children: [
          RoundSurfaceButton(
            size: 42,
            onTap: () => _scaffoldKey.currentState?.openDrawer(),
            child: const HamburgerIcon(),
          ),
          const SizedBox(width: 12),
          Container(
            height: 42,
            padding: const EdgeInsets.symmetric(horizontal: 20),
            decoration: BoxDecoration(
              color: Colors.white,
              borderRadius: BorderRadius.circular(21),
            ),
            alignment: Alignment.center,
            child: const Text(
              'ChatGPT',
              style: TextStyle(
                fontSize: 16,
                fontWeight: FontWeight.w600,
                letterSpacing: -0.3,
                color: Color(0xFF1C1C1E),
              ),
            ),
          ),
          const Spacer(),
          RoundSurfaceButton(
            size: 42,
            onTap: _clearChat,
            child: const Icon(
              Icons.edit_outlined,
              size: 20,
              color: Color(0xFF1C1C1E),
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildMessages() {
    return Expanded(
      child: ListView.separated(
        controller: _scrollController,
        padding: const EdgeInsets.fromLTRB(16, 24, 16, 20),
        physics: const BouncingScrollPhysics(),
        itemCount: _messages.length,
        separatorBuilder: (_, __) => const SizedBox(height: 18),
        itemBuilder: (context, index) {
          final message = _messages[index];
          return MessageEntrance(
            key: ValueKey(message.id),
            child: MessageBubble(
              message: message,
              onCopy: () => _copyAssistantText(message.text),
              onActionTap: _showComingSoon,
            ),
          );
        },
      ),
    );
  }

  Widget _buildComposer() {
    return SafeArea(
      top: false,
      child: Padding(
        padding: const EdgeInsets.fromLTRB(16, 8, 16, 12),
        child: Row(
          crossAxisAlignment: CrossAxisAlignment.end,
          children: [
            RoundSurfaceButton(
              size: 44,
              margin: const EdgeInsets.only(bottom: 2),
              onTap: _showToolSheet,
              child: const Icon(
                Icons.auto_awesome_rounded,
                size: 22,
                color: Color(0xFF0D0D0D),
              ),
            ),
            const SizedBox(width: 12),
            Expanded(
              child: AnimatedContainer(
                duration: const Duration(milliseconds: 220),
                curve: Curves.easeOutCubic,
                padding: EdgeInsets.fromLTRB(
                  18,
                  _selectedTool == null ? 6 : 10,
                  8,
                  6,
                ),
                decoration: BoxDecoration(
                  color: Colors.white,
                  borderRadius: BorderRadius.circular(24),
                ),
                child: Column(
                  mainAxisSize: MainAxisSize.min,
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    AnimatedSwitcher(
                      duration: const Duration(milliseconds: 180),
                      child: _selectedTool == null
                          ? const SizedBox.shrink()
                          : Padding(
                              key: ValueKey(_selectedTool!.name),
                              padding: const EdgeInsets.only(bottom: 8),
                              child: SelectedToolChip(
                                tool: _selectedTool!,
                                onClear: () {
                                  setState(() {
                                    _selectedTool = null;
                                  });
                                },
                              ),
                            ),
                    ),
                    Row(
                      crossAxisAlignment: CrossAxisAlignment.end,
                      children: [
                        Expanded(
                          child: TextField(
                            controller: _textController,
                            focusNode: _focusNode,
                            minLines: 1,
                            maxLines: 5,
                            style: const TextStyle(
                              fontSize: 17,
                              height: 1.45,
                              color: Color(0xFF1C1C1E),
                            ),
                            decoration: const InputDecoration(
                              hintText: 'Message',
                              hintStyle: TextStyle(
                                fontSize: 17,
                                color: Color(0xFF9CA3AF),
                              ),
                              isCollapsed: true,
                              border: InputBorder.none,
                            ),
                          ),
                        ),
                        const SizedBox(width: 8),
                        AnimatedOpacity(
                          duration: const Duration(milliseconds: 150),
                          opacity: _canSend ? 1 : 0.4,
                          child: IgnorePointer(
                            ignoring: !_canSend,
                            child: GestureDetector(
                              onTap: _sendMessage,
                              child: Container(
                                width: 36,
                                height: 36,
                                margin: const EdgeInsets.only(bottom: 2),
                                decoration: const BoxDecoration(
                                  color: Color(0xFF1C1C1E),
                                  shape: BoxShape.circle,
                                ),
                                child: const Icon(
                                  Icons.arrow_upward_rounded,
                                  size: 18,
                                  color: Colors.white,
                                ),
                              ),
                            ),
                          ),
                        ),
                      ],
                    ),
                  ],
                ),
              ),
            ),
          ],
        ),
      ),
    );
  }
}
