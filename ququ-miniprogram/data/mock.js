const activities = [
  {
    id: 1,
    title: "奥森夜跑 5km 轻训练",
    category: "运动健身",
    status: "报名中",
    cover: "linear-green",
    time: "6月30日 19:30",
    deadline: "6月30日 12:00",
    location: "奥林匹克森林公园南门",
    distance: "2.4km",
    city: "北京",
    fee: "免费",
    joined: 18,
    capacity: 30,
    organizer: "北城跑团",
    tags: ["夜跑", "新手友好", "AA"],
    desc: "配速 6:30-7:30，路线平缓，集合后统一热身。适合想恢复训练或认识跑友的同学。",
    safety: "请穿着运动鞋，携带饮用水；身体不适请及时退出。",
    lat: 40.016,
    lng: 116.391
  },
  {
    id: 2,
    title: "周末桌游局：璀璨宝石 + 风声",
    category: "桌游聚会",
    status: "余位紧张",
    cover: "linear-yellow",
    time: "7月4日 14:00",
    deadline: "7月3日 20:00",
    location: "五道口 FunTable 桌游吧",
    distance: "4.8km",
    city: "北京",
    fee: "39元",
    joined: 10,
    capacity: 12,
    organizer: "Onlyfriends 桌游小队",
    tags: ["桌游", "破冰", "室内"],
    desc: "主打轻策略和社交推理，现场会有人带规则，新人可以直接来。",
    safety: "活动为室内公开场地，请保管好个人物品。",
    lat: 39.992,
    lng: 116.337
  },
  {
    id: 3,
    title: "胡同影像采风：从鼓楼到什刹海",
    category: "城市探索",
    status: "报名中",
    cover: "linear-blue",
    time: "7月6日 16:00",
    deadline: "7月5日 22:00",
    location: "鼓楼地铁站 G 口",
    distance: "6.1km",
    city: "北京",
    fee: "免费",
    joined: 22,
    capacity: 40,
    organizer: "城市漫游社",
    tags: ["摄影", "Citywalk", "日落"],
    desc: "沿途会经过老字号、胡同院落和湖边落日点，适合手机摄影与人文观察。",
    safety: "路线包含步行，请穿舒适鞋；拍摄时注意行人隐私。",
    lat: 39.947,
    lng: 116.393
  }
];

const teams = [
  { id: 1, name: "北城跑团", members: 268, topic: "每周二/四固定夜跑", tags: ["运动", "长期活动"] },
  { id: 2, name: "Onlyfriends 桌游小队", members: 96, topic: "轻策、推理、聚会破冰", tags: ["桌游", "室内"] },
  { id: 3, name: "城市漫游社", members: 134, topic: "发现北京的好看路线", tags: ["摄影", "Citywalk"] }
];

const conversations = [
  { id: 1, avatarText: "桌", name: "周末桌游局群聊", last: "本周会开一桌璀璨宝石，规则我提前发。", time: "12:48", unread: 3 },
  { id: 2, avatarText: "跑", name: "北城跑团", last: "今晚风大，集合点改到南门便利店旁。", time: "09:20", unread: 0 },
  { id: 3, avatarText: "林", name: "林澈", last: "你报名那个 Citywalk 了吗？", time: "昨天", unread: 1 }
];

const templates = [
  { type: "运动健身", title: "轻量运动局", desc: "适合跑步、飞盘、羽毛球等活动" },
  { type: "桌游聚会", title: "室内破冰局", desc: "自动补齐人数、费用和安全提示" },
  { type: "城市探索", title: "Citywalk 路线", desc: "适合摄影、探店和文化路线" },
  { type: "学习交流", title: "学习共创会", desc: "适合读书、技术分享和备考搭子" }
];

module.exports = {
  activities,
  teams,
  conversations,
  templates
};
