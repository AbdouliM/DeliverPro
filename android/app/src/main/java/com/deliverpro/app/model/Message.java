package com.deliverpro.app.model;

import com.google.gson.annotations.SerializedName;

public class Message {

    // ── Individual message fields
    @SerializedName("message_id")   private int     messageId;
    @SerializedName("sender_id")    private int     senderId;
    @SerializedName("receiver_id")  private int     receiverId;
    @SerializedName("content")      private String  content;
    @SerializedName("is_read")      private int     isRead;
    @SerializedName("sent_at")      private String  sentAt;
    @SerializedName("is_mine")      private boolean isMine;

    // ── Conversation list fields
    @SerializedName("user_id")      private int     userId;
    @SerializedName("full_name")    private String  fullName;
    @SerializedName("role")         private String  role;
    @SerializedName("last_message") private String  lastMessage;
    @SerializedName("last_sent_at") private String  lastSentAt;
    @SerializedName("unread_count") private int     unreadCount;

    // ── Runtime only — not from API
    private boolean isUrgent = false;

    // No-arg constructor needed to create synthetic alert entries
    public Message() {}

    // ── Getters
    public int     getMessageId()   { return messageId;   }
    public int     getSenderId()    { return senderId;    }
    public int     getReceiverId()  { return receiverId;  }
    public String  getContent()     { return content;     }
    public String  getSentAt()      { return sentAt;      }
    public boolean isMine()         { return isMine;      }
    public int     getUserId()      { return userId;      }
    public String  getFullName()    { return fullName;    }
    public String  getRole()        { return role;        }
    public String  getLastMessage() { return lastMessage; }
    public String  getLastSentAt()  { return lastSentAt;  }
    public int     getUnreadCount() { return unreadCount; }
    public boolean isUrgent()       { return isUrgent;    }

    // ── Setters (runtime mutations + synthetic entry construction)
    public void setUserId(int id)          { this.userId      = id;  }
    public void setFullName(String name)   { this.fullName    = name; }
    public void setUrgent(boolean urgent)  { this.isUrgent    = urgent; }
    public void setLastMessage(String msg) { this.lastMessage = msg;  }
    public void setLastSentAt(String at)   { this.lastSentAt  = at;   }
}
/*package com.deliverpro.app.model;

import com.google.gson.annotations.SerializedName;

public class Message {

    // ── Fields for individual messages (GET /messages/:userId, POST /messages)
    @SerializedName("message_id")   private int     messageId;
    @SerializedName("sender_id")    private int     senderId;
    @SerializedName("receiver_id")  private int     receiverId;
    @SerializedName("content")      private String  content;
    @SerializedName("is_read")      private int     isRead;
    @SerializedName("sent_at")      private String  sentAt;
    @SerializedName("is_mine")      private boolean isMine;

    // ── Fields for conversations list (GET /messages/conversations)
    @SerializedName("user_id")      private int     userId;       // contact's ID — used as receiver_id when opening chat
    @SerializedName("full_name")    private String  fullName;
    @SerializedName("role")         private String  role;
    @SerializedName("last_message") private String  lastMessage;  // preview text in conversation list
    @SerializedName("last_sent_at") private String  lastSentAt;   // timestamp of last message
    @SerializedName("unread_count") private int     unreadCount;

    // ── Getters for individual messages
    public int     getMessageId()   { return messageId;   }
    public int     getSenderId()    { return senderId;    }
    public int     getReceiverId()  { return receiverId;  }
    public String  getContent()     { return content;     }
    public String  getSentAt()      { return sentAt;      }
    public boolean isMine()         { return isMine;      }

    // ── Getters for conversations
    public int     getUserId()      { return userId;      }
    public String  getFullName()    { return fullName;    }
    public String  getRole()        { return role;        }
    public String  getLastMessage() { return lastMessage; }
    public String  getLastSentAt()  { return lastSentAt;  }
    public int     getUnreadCount() { return unreadCount; }
}/*
// ── Message.java ─────────────────────────────────────────────
package com.deliverpro.app.model;
import com.google.gson.annotations.SerializedName;
public class Message {
    @SerializedName("message_id")  private int     messageId;
    @SerializedName("sender_id")   private int     senderId;
    @SerializedName("receiver_id") private int     receiverId;
    @SerializedName("content")     private String  content;
    @SerializedName("is_read")     private int     isRead;
    @SerializedName("sent_at")     private String  sentAt;
    @SerializedName("is_mine")     private boolean isMine;
    @SerializedName("full_name")   private String  fullName;
    @SerializedName("unread_count")private int     unreadCount;

    public int     getMessageId()   { return messageId;   }
    public int     getSenderId()    { return senderId;    }
    public int     getReceiverId()  { return receiverId;  }
    public String  getContent()     { return content;     }
    public String  getSentAt()      { return sentAt;      }
    public boolean isMine()         { return isMine;      }
    public String  getFullName()    { return fullName;    }
    public int     getUnreadCount() { return unreadCount; }
}*/