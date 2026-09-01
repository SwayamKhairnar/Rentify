import { useState, useEffect, useRef } from 'react';
import { chatService } from '../services/chat.service';
import { websocketService } from '../services/websocket.service';
import { useAuth } from '../hooks/useAuth';
import { useToast } from '../hooks/useToast';
import LoadingSpinner from '../components/LoadingSpinner';
import Toast from '../components/Toast';
import { Send, MessageCircle } from 'lucide-react';
import { timeAgo } from '../utils/helpers';
import './Chat.css';

/**
 * Chat page — displays conversations list and active chat thread.
 */
export default function Chat() {
  const { user } = useAuth();
  const { toast, showToast } = useToast();
  const messagesEndRef = useRef(null);

  const [conversations, setConversations] = useState([]);
  const [activeConvo, setActiveConvo] = useState(null);
  const [messages, setMessages] = useState([]);
  const [newMessage, setNewMessage] = useState('');
  const [loading, setLoading] = useState(true);
  const [sending, setSending] = useState(false);

  useEffect(() => {
    fetchConversations();
  }, []);

  useEffect(() => {
    if (activeConvo) {
      const convoId = activeConvo._id || activeConvo.id;
      fetchMessages(convoId);

      const unsubscribe = websocketService.subscribeToConversation(convoId, (incomingMsg) => {
        setMessages((prev) => {
          const msgId = incomingMsg._id || incomingMsg.id;
          if (prev.some((m) => (m._id || m.id) === msgId)) return prev;
          return [...prev, incomingMsg];
        });
        fetchConversations();
      });

      return () => {
        if (unsubscribe) unsubscribe();
      };
    }
  }, [activeConvo]);

  useEffect(() => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [messages]);

  async function fetchConversations() {
    try {
      const res = await chatService.getConversations();
      setConversations(res.data.conversations || []);
    } catch (err) {
      console.error('Failed to load conversations');
    } finally {
      setLoading(false);
    }
  }

  async function fetchMessages(convoId) {
    try {
      const res = await chatService.getMessages(convoId);
      setMessages(res.data.messages || []);
    } catch (err) {
      showToast('Failed to load messages', 'error');
    }
  }

  async function handleSend(e) {
    e.preventDefault();
    if (!newMessage.trim() || !activeConvo) return;
    setSending(true);
    try {
      await chatService.sendMessage(activeConvo._id, newMessage.trim());
      setNewMessage('');
      fetchMessages(activeConvo._id);
      fetchConversations();
    } catch (err) {
      showToast(err.message, 'error');
    } finally {
      setSending(false);
    }
  }

  /**
   * Gets the other participant's name in a conversation.
   */
  function getOtherParticipant(convo) {
    if (!convo.participants) return 'Unknown';
    const other = convo.participants.find((p) => p._id !== user._id);
    return other?.name || 'Unknown';
  }

  if (loading) return <LoadingSpinner />;

  return (
    <div className="page chat-page">
      {toast && <Toast message={toast.message} type={toast.type} />}

      <div className="container">
        <div className="chat-layout" id="chat-layout">
          {/* Conversations Sidebar */}
          <div className={`chat-sidebar ${activeConvo ? 'hide-mobile' : ''}`}>
            <h2 className="chat-sidebar-title">Messages</h2>
            {conversations.length === 0 ? (
              <div className="empty-state" style={{ padding: '32px 16px' }}>
                <MessageCircle size={32} style={{ marginBottom: '12px', opacity: 0.4 }} />
                <p>No conversations yet</p>
              </div>
            ) : (
              <div className="convo-list">
                {conversations.map((convo) => (
                  <button
                    key={convo._id}
                    className={`convo-item ${activeConvo?._id === convo._id ? 'active' : ''}`}
                    onClick={() => setActiveConvo(convo)}
                  >
                    <div className="convo-avatar">
                      {getOtherParticipant(convo).charAt(0).toUpperCase()}
                    </div>
                    <div className="convo-info">
                      <div className="convo-name">{getOtherParticipant(convo)}</div>
                      <div className="convo-last">
                        {convo.lastMessage || 'No messages yet'}
                      </div>
                    </div>
                    <div className="convo-time">
                      {convo.lastMessageAt && timeAgo(convo.lastMessageAt)}
                    </div>
                  </button>
                ))}
              </div>
            )}
          </div>

          {/* Chat Window */}
          <div className={`chat-window ${!activeConvo ? 'hide-mobile' : ''}`}>
            {activeConvo ? (
              <>
                <div className="chat-header">
                  <button
                    className="btn btn-ghost btn-sm chat-back-mobile"
                    onClick={() => setActiveConvo(null)}
                  >
                    ←
                  </button>
                  <div className="chat-header-name">
                    {getOtherParticipant(activeConvo)}
                  </div>
                </div>

                <div className="messages-container">
                  {messages.map((msg) => (
                    <div
                      key={msg._id}
                      className={`message ${msg.sender?._id === user._id ? 'mine' : 'theirs'}`}
                    >
                      <div className="message-bubble">
                        {msg.content}
                      </div>
                      <div className="message-time">{timeAgo(msg.createdAt)}</div>
                    </div>
                  ))}
                  <div ref={messagesEndRef} />
                </div>

                <form className="chat-input-bar" onSubmit={handleSend}>
                  <input
                    type="text"
                    className="form-input chat-input"
                    placeholder="Type a message..."
                    value={newMessage}
                    onChange={(e) => setNewMessage(e.target.value)}
                    id="chat-message-input"
                  />
                  <button
                    type="submit"
                    className="btn btn-primary"
                    disabled={sending || !newMessage.trim()}
                    id="send-message-btn"
                  >
                    <Send size={18} />
                  </button>
                </form>
              </>
            ) : (
              <div className="chat-placeholder">
                <MessageCircle size={48} style={{ opacity: 0.3, marginBottom: '16px' }} />
                <h3>Select a conversation</h3>
                <p>Choose a conversation from the sidebar to start chatting</p>
              </div>
            )}
          </div>
        </div>
      </div>
    </div>
  );
}
